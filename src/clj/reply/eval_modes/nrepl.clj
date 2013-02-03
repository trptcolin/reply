(ns reply.eval-modes.nrepl
  (:import [java.util.concurrent LinkedBlockingQueue
                                 TimeUnit])
  (:require [clojure.main]
            [clojure.tools.nrepl.cmdline :as nrepl.cmdline]
            [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.misc :as nrepl.misc]
            [clojure.tools.nrepl.server :as nrepl.server]
            [clojure.tools.nrepl.transport :as nrepl.transport]
            [reply.exit]
            [reply.eval-state :as eval-state]
            [reply.initialization]
            [reply.reader.jline :as reader.jline]
            [reply.signals :as signals]
            [net.cgrand.sjacket :as sjacket]
            [net.cgrand.sjacket.parser :as sjacket.parser]))

(def current-command-id (atom nil))
(def current-session (atom nil))
(def current-ns (atom (str *ns*)))

(defn handle-client-interruption! [client]
  (signals/set-signal-handler!
    "INT"
    (fn [sig]
      (reader.jline/print-interruption)
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

(defn safe-read-line [input-stream]
  (try (.readLine input-stream)
    (catch jline.console.UserInterruptException e
      :interrupted)))

(defn execute-with-client [client options form]
  (let [command-id (nrepl.misc/uuid)
        session (or (:session options) @current-session)
        session-sender (nrepl/client-session client :session session)]
    (session-sender {:op "eval" :code form :id command-id})
    (reset! current-command-id command-id)
    (doseq [{:keys [ns value out err] :as res}
            (take-while
              #(not (and (= command-id (:id %))
                         (some #{"done" "interrupted" "error"} (:status %))))
              (filter identity (session-responses session)))]
      (when (some #{"need-input"} (:status res))
        (let [input-result (safe-read-line *in*)]
          (when-not (= :interrupted input-result)
            (session-sender
              {:op "stdin" :stdin (str input-result "\n")
               :id (nrepl.misc/uuid)}))))
      (when value ((:value options print) value))
      (flush)
      (when (and ns (not (:session options)))
        (reset! current-ns ns)))
    (when (:interactive options) (println))
    (reset! current-command-id nil)
    @current-ns))

(defn parsed-forms
  ([request-exit] (parsed-forms request-exit nil))
  ([request-exit text-so-far]
   (if-let [next-text (safe-read-line *in*)]
     (let [interrupted? (= :interrupted next-text)
           parse-tree (when-not interrupted?
                        (sjacket.parser/parser
                          (if text-so-far
                            (str text-so-far \newline next-text)
                            next-text)))]
       (if (or interrupted? (empty? (:content parse-tree)))
         (list "")
         (let [completed? (fn [node]
                            (or (not= :net.cgrand.parsley/unfinished (:tag node))
                                (some #(= :net.cgrand.parsley/unexpected (:tag %))
                                      (tree-seq :tag :content node))))
               complete-forms (take-while completed? (:content parse-tree))
               remainder (drop-while completed? (:content parse-tree))
               form-strings (map sjacket/str-pt
                                 (remove #(contains? #{:whitespace :comment :discard}
                                                     (:tag %))
                                         complete-forms))]
           (cond (seq remainder)
                   (lazy-seq
                     (concat form-strings
                             (parsed-forms request-exit
                                           (apply str (map sjacket/str-pt remainder)))))
                 (seq form-strings)
                   form-strings
                 :else
                   (list "")))))
     (list request-exit))))

(defn run-repl
  ([connection] (run-repl connection nil))
  ([connection {:keys [prompt] :as options}]
    (let [{:keys [major minor incremental qualifier]} *clojure-version*]
      (loop [ns (execute-with-client connection options "")]
        (prompt ns)
        (flush)
        (let [eof (Object.)
              execute (partial execute-with-client connection
                               (assoc options :interactive true))
              forms (parsed-forms eof)]
          (if (reply.exit/done? eof (first forms))
            nil
            (recur (last (doall (map execute forms))))))))))

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
  (try (when-let [{:keys [out err] :as resp} (nrepl.transport/recv connection 100)]
         (when err (print err))
         (when out (print out))
         (when-not (or err out)
           (.offer ^LinkedBlockingQueue (@response-queues (:session resp)) resp))
         (flush))
    (catch Throwable t
      (clojure.repl/pst t)
      (reply.exit/exit)))
  (recur connection))

(defn main
  "Mostly ripped from nREPL's cmdline namespace."
  [options]
  (let [connection         (get-connection options)
        client             (nrepl/client connection Long/MAX_VALUE)
        session            (nrepl/new-session client)
        completion-session (nrepl/new-session client)]
    (reset! current-session session)
    (swap! response-queues assoc session (LinkedBlockingQueue.))
    (swap! response-queues assoc completion-session (LinkedBlockingQueue.))
    (let [options (assoc options :prompt
                    (fn [ns]
                      (reader.jline/prepare-for-read
                        (partial completion-eval client completion-session)
                        ns)))
          options (if (:color options)
                    (merge options nrepl.cmdline/colored-output)
                    options)]

      (.start (Thread. (partial poll-for-responses connection)))
      (execute-with-client
               client
               (assoc options :value (constantly nil))
               (binding [*print-length* nil
                         *print-level* nil]
                 (pr-str (list 'do
                           (reply.initialization/export-definition
                             'reply.signals/set-signal-handler!)
                           '(set-signal-handler! "INT" (fn [s]))
                           (reply.initialization/construct-init-code options)))))

      (handle-client-interruption! client)
      (run-repl client options))))

