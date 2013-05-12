(ns reply.eval-modes.nrepl
  (:import [java.util.concurrent LinkedBlockingQueue TimeUnit])
  (:require [clojure.main]
            [clojure.tools.nrepl.cmdline :as nrepl.cmdline]
            [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.misc :as nrepl.misc]
            [clojure.tools.nrepl.server :as nrepl.server]
            [clojure.tools.nrepl.transport :as nrepl.transport]
            [reply.exit]
            [reply.eval-state :as eval-state]
            [reply.initialization]
            [reply.parsing :as parsing]
            [reply.reader.simple-jline :as simple-jline]
            [reply.signals :as signals]))

(def current-command-id (atom nil))
(def current-session (atom nil))
(def current-ns (atom (str *ns*)))

(defn handle-client-interruption! [client]
  (signals/set-signal-handler!
    "INT"
    (fn [sig]
      (when-let [command-id @current-command-id]
        (client {:op "interrupt"
                 :session @current-session
                 :interrupt-id command-id})))))

(def response-queues (atom {}))

(defn session-responses [session]
  (lazy-seq
    (cons (.poll ^LinkedBlockingQueue (@response-queues session)
                 50
                 TimeUnit/MILLISECONDS)
          (session-responses session))))

(defn execute-with-client [client options form]
  (let [command-id (nrepl.misc/uuid)
        session (or (:session options) @current-session)
        session-sender (nrepl/client-session client :session session)
        message-to-send {:op "eval" :code form :id command-id}
        read-input-line-fn (:read-input-line-fn options)]
    (session-sender message-to-send)
    (reset! current-command-id command-id)
    (doseq [{:keys [ns value out err] :as res}
            (take-while
              #(not (and (= command-id (:id %))
                         (some #{"done" "interrupted" "error" "eval-error"}
                               (:status %))))
              (filter identity (session-responses session)))]
      (when (some #{"need-input"} (:status res))
        (reset! current-command-id nil)
        (let [input-result (read-input-line-fn)
              in-message-id (nrepl.misc/uuid)
              message {:op "stdin" :stdin (str input-result "\n")
                       :id in-message-id}]
          (session-sender message)
          (reset! current-command-id command-id)))
      (when value ((:value options print) value))
      (flush)
      (when (and ns (not (:session options)))
        (reset! current-ns ns)))
    (when (:interactive options) (println))
    (reset! current-command-id nil)
    @current-ns))

(defn- handle-ns-init-error [ns connection options]
  (if (= ns "reply.eval-modes.nrepl")
    (let [fallback-ns
          (execute-with-client
            connection options
            "(in-ns 'user)
            (println \"\\nError loading namespace; falling back to user\")")]
      (println)
      fallback-ns)
    ns))

(defn run-repl
  ([connection] (run-repl connection nil))
  ([connection {:keys [prompt subsequent-prompt history-file
                       input-stream output-stream read-line-fn]
                :as options}]
   (loop [ns (execute-with-client connection options "")]
     (let [ns (handle-ns-init-error ns connection options)
           eof (Object.)
           execute (partial execute-with-client connection
                            (assoc options :interactive true))
           forms (parsing/parsed-forms
                   {:request-exit eof
                    :prompt-string (prompt ns)
                    :ns ns
                    :read-line-fn read-line-fn
                    :history-file history-file
                    :input-stream input-stream
                    :output-stream output-stream
                    :subsequent-prompt-string (subsequent-prompt ns)
                    :text-so-far nil})]
       (if (reply.exit/done? eof (first forms))
         nil
         (recur (last (doall (map execute forms)))))))))

;; TODO: this could be less convoluted if we could break backwards-compat
(defn- url-for [attach host port]
  (if (and attach (re-find #"^\w+://" attach))
    attach
    (let [[port host] (if attach
                        (reverse (.split ^String attach ":"))
                        [port host])]
      (format "nrepl://%s:%s" (or host "localhost") port))))

(defn- load-drawbridge
  "Load Drawbridge (and therefore get its HTTP/HTTPS multimethods
   registered) if it's available."
  []
  (try
    (require '[cemerick.drawbridge.client])
    (catch Exception e)))

(defn get-connection [{:keys [attach host port]}]
  (let [port (if-not attach
               (-> (nrepl.server/start-server :port (Integer. (or port 0)))
                   deref :ss .getLocalPort))
        url (url-for attach host port)]
    (when (-> url java.net.URI. .getScheme .toLowerCase #{"http" "https"})
      (load-drawbridge))
    (nrepl/url-connect url)))

(defn completion-eval [client session form]
  (let [results (atom "nil")]
    (execute-with-client
      client
      {:value (partial reset! results)
       :session session}
      (binding [*print-length* nil
                *print-level* nil]
        (pr-str `(binding [*ns* (symbol ~(deref current-ns))] ~form))))
    (read-string @results)))

(defn poll-for-responses [connection]
  (try (when-let [{:keys [out err] :as resp}
                  (nrepl.transport/recv connection 100)]
         (when err (print err))
         (when out (print out))
         (when-not (or err out)
           (.offer ^LinkedBlockingQueue (@response-queues (:session resp))
                   resp))
         (flush))
    (catch Throwable t
      (clojure.repl/pst t)
      (reply.exit/exit)))
  (recur connection))

(defn ->fn [config default]
  (cond (fn? config) config
        (seq? config) (eval config)
        :else default))

(defn main
  [options]
  (let [connection         (get-connection options)
        client             (nrepl/client connection Long/MAX_VALUE)
        session            (nrepl/new-session client)
        completion-session (nrepl/new-session client)]
    (reset! current-session session)
    (swap! response-queues assoc
           session (LinkedBlockingQueue.)
           completion-session (LinkedBlockingQueue.))
    (let [custom-prompt (:custom-prompt options)
          subsequent-prompt (:subsequent-prompt options)
          options (assoc options :prompt
                         (->fn custom-prompt (fn [ns] (str ns "=> "))))
          options (assoc options :subsequent-prompt
                         (->fn subsequent-prompt (constantly nil)))
          options (if (:color options)
                    (merge options nrepl.cmdline/colored-output)
                    options)
          completion-eval-fn (partial completion-eval client completion-session)
          options (assoc options
                         :read-line-fn
                           (partial
                             simple-jline/safe-read-line
                             completion-eval-fn)
                         :read-input-line-fn
                           (partial
                             simple-jline/safe-read-line
                             {:no-jline true :prompt-string ""}))]
      (.start (Thread.
                (bound-fn [] (poll-for-responses connection))))
      (execute-with-client
               client
               (assoc options :value (constantly nil))
               (binding [*print-length* nil
                         *print-level* nil]
                 (pr-str (list 'do
                           (reply.initialization/export-definition
                             'reply.signals/set-signal-handler!)
                           '(set-signal-handler! "INT" (fn [s]))
                           (reply.initialization/construct-init-code
                             options)))))
      (handle-client-interruption! client)
      (run-repl client options))))

