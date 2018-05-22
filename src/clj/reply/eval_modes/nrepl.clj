(ns reply.eval-modes.nrepl
  (:import [java.util.concurrent LinkedBlockingQueue TimeUnit]
           [java.net ServerSocket])
  (:require [clojure.main]
            [nrepl.core :as nrepl]
            [nrepl.misc :as nrepl.misc]
            [nrepl.server :as nrepl.server]
            [nrepl.transport :as nrepl.transport]
            [reply.exit]
            [reply.eval-modes.shared :as eval-modes.shared]
            [reply.eval-state :as eval-state]
            [reply.initialization]
            [reply.reader.simple-jline :as simple-jline]
            [reply.signals :as signals]))

(def current-command-id (atom nil))
(def current-session (atom nil))
(def current-connection (atom nil))
(def current-ns (atom (str *ns*)))
(def nrepl-server (atom nil))
(def response-poller (atom nil))

(defn reset-nrepl-state! []
  (when @nrepl-server
    (nrepl.server/stop-server @nrepl-server))
  (when (isa? (class @current-connection) java.io.Closeable)
    (.close ^java.io.Closeable @current-connection))
  (signals/set-signal-handler! "INT" (constantly nil))
  (reset! current-command-id nil)
  (reset! current-session nil)
  (reset! current-connection nil)
  (reset! nrepl-server nil)
  (reset! response-poller nil))

(defn handle-client-interruption! [client]
  (signals/set-signal-handler!
    "INT"
    (fn [sig]
      (when-let [command-id @current-command-id]
        (client {:op "interrupt"
                 :session @current-session
                 :interrupt-id command-id})))))

(def response-queues (atom {}))

(defn notify-all-queues-of-error [e]
  (doseq [session-key (keys @response-queues)]
    (.offer ^LinkedBlockingQueue (@response-queues session-key)
            {:status ["error"] :global true :error e})))

(defn session-responses [session]
  (lazy-seq
    (cons (.poll ^LinkedBlockingQueue (@response-queues session)
                 50
                 TimeUnit/MILLISECONDS)
          (session-responses session))))

(declare execute-with-client)
(defn- end-of-stream? [client options command-id message]
  (let [relevant-message (or (= command-id (:id message)) (:global message))
        error (some #{"error" "eval-error"} (:status message))
        done (some #{"done" "interrupted"} (:status message))]
    (when error
      (let [caught (:caught options)]
        (when (or (symbol? caught) (list? caught))
          (execute-with-client client options (str "(" (pr-str caught) ")"))))
      (when (:global message)
        (throw (:error message))))

    (and relevant-message (or error done))))

(defn execute-with-client [client options form]
  (let [command-id (nrepl.misc/uuid)
        session (or (:session options) @current-session)
        session-sender (nrepl/client-session client :session session)
        message-to-send (merge (get-in options [:nrepl-context :interactive-eval])
                               {:op "eval" :code form :id command-id})
        read-input-line-fn (:read-input-line-fn options)]
    (session-sender message-to-send)
    (reset! current-command-id command-id)
    (doseq [{:keys [ns value out err] :as res}
            (take-while
              #(not (end-of-stream? client options command-id %))
              (filter identity (session-responses session)))]
      (when (some #{"need-input"} (:status res))
        (reset! current-command-id nil)
        (let [input-result (read-input-line-fn)
              in-message-id (nrepl.misc/uuid)
              message {:op "stdin" :stdin (when input-result
                                            (str input-result "\n"))
                       :id in-message-id}]
          (session-sender message)
          (reset! current-command-id command-id)))
      (when value ((:print-value options) value))
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
   (loop [ns (let [ns (execute-with-client connection options "")]
               (handle-ns-init-error ns connection options))]
     (let [eof (Object.)
           execute (partial execute-with-client connection
                            (assoc options :interactive true))
           pf-opts {:request-exit eof
                    :prompt-string (prompt ns)
                    :ns ns
                    :read-line-fn read-line-fn
                    :history-file history-file
                    :input-stream input-stream
                    :output-stream output-stream
                    :subsequent-prompt-string (subsequent-prompt ns)
                    :text-so-far nil}
           parsed-forms-fn (eval-modes.shared/load-parsed-forms-fn-in-background)
           read-text (read-line-fn pf-opts)
           forms (@parsed-forms-fn read-text pf-opts)]
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
    (require '[drawbridge.client])
    (catch Exception e)))

(defn get-connection [{:keys [attach host port]}]
  (let [server (when-not attach
                 (nrepl.server/start-server
                   :port (Integer/parseInt (str (or port 0)))))
        port (when-not attach
               (let [^ServerSocket socket (-> server deref :ss)]
                 (.getLocalPort socket)))
        url (url-for attach host port)]
    (when server
      (reset! nrepl-server server))
    (when (-> url java.net.URI. .getScheme .toLowerCase #{"http" "https"})
      (load-drawbridge))
    (nrepl/url-connect url)))

(defn completion-eval [client session form]
  (let [results (atom "nil")]
    (execute-with-client
      client
      {:print-value (partial reset! results)
       :session session}
      (binding [*print-length* nil
                *print-level* nil]
        (pr-str `(binding [*ns* (the-ns (symbol ~(deref current-ns)))] ~form))))
    (read-string @results)))

(defn poll-for-responses [{:keys [print-out print-err] :as options} connection]
  (let [continue
        (try
          (when-let [{:keys [out err] :as resp}
                     (nrepl.transport/recv connection 100)]
            (when err ((or print-err print) err))
            (when out ((or print-out print) out))
            (when-not (or err out)
              (.offer ^LinkedBlockingQueue (@response-queues (:session resp))
                      resp))
            (flush))
          :success
          (catch Throwable t
            (notify-all-queues-of-error t)
            (when (System/getenv "DEBUG") (clojure.repl/pst t))
            :failure))]
    (when (= :success continue)
      (recur options connection))))

(defn main
  [options]
  (let [connection         (get-connection options)
        client             (nrepl/client connection Long/MAX_VALUE)
        session            (nrepl/new-session client)
        completion-session (nrepl/new-session client)]
    (reset! current-connection connection)
    (reset! current-session session)
    (swap! response-queues assoc
           session (LinkedBlockingQueue.)
           completion-session (LinkedBlockingQueue.))
    (let [options (eval-modes.shared/set-default-options options)
          completion-eval-fn (partial completion-eval client completion-session)
          options (assoc options
                         :read-line-fn
                         (partial
                           simple-jline/safe-read-line
                           completion-eval-fn))]

      (let [^Runnable operation (bound-fn [] (poll-for-responses options connection))]
        (reset! response-poller (Thread. operation)))
      (doto ^Thread @response-poller
        (.setName "nREPL response poller")
        (.setDaemon true)
        (.start))
      (completion-eval-fn '(set! *print-length* nil))
      (execute-with-client
               client
               (assoc options :print-value (constantly nil))
               (binding [*print-length* nil
                         *print-level* nil]
                 (pr-str (list 'do
                           (reply.initialization/construct-init-code
                             options)))))
      (handle-client-interruption! client)
      (run-repl client options)
      (reset-nrepl-state!)
      (simple-jline/shutdown))))
