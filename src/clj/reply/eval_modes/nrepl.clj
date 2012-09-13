(ns reply.eval-modes.nrepl
  (:import [java.util.concurrent ConcurrentLinkedQueue])
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
    (cons (.poll (@response-queues session))
          (session-responses session))))

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
        (let [input-result (.readLine *in*)]
          (.clearRawInput *in*)
          (session-sender
            {:op "stdin" :stdin (str input-result "\n")
             :id (nrepl.misc/uuid)})))
      (when value ((:value options print) value))
      (flush)
      (when (and ns (not (:session options)))
        (reset! current-ns ns)))
    (when (:interactive options) (println))
    (reset! current-command-id nil)
    @current-ns))

(defn repl-read [request-prompt request-exit read-error]
  (if-let [start-or-end ({:line-start request-prompt :stream-end request-exit}
                         (clojure.main/skip-whitespace *in*))]
    [[] start-or-end]
    (try
      (let [input (clojure.core/read *in*)]
        (clojure.main/skip-if-eol *in*)
        (let [raw-input (.getRawInput *in*)]
          (.clearRawInput *in*)
          [raw-input input]))
      (catch Exception e
        (clojure.main/skip-if-eol *in*)
        (.clearRawInput *in*)
        [e read-error]))))

(defn repl-parse [request-prompt request-exit read-error]
  (loop [text-so-far nil]
    (if-let [next-text (.readLine *in*)]
      (let [concatted-text (if text-so-far
                             (str text-so-far \newline next-text)
                             next-text)
            parse-tree (sjacket.parser/parser concatted-text)
            completed? #(not= :net.cgrand.parsley/unfinished (:tag %))]
        (cond (not (completed? parse-tree))
                (recur concatted-text)
              (empty? (:content parse-tree))
                (recur concatted-text)
              :else
                (let [complete-forms (take-while completed? (:content parse-tree))
                      remainder (drop-while completed? (:content parse-tree))]
                  [(map sjacket/str-pt complete-forms) {:remainder remainder}])))
      [[] request-exit])))

(defn run-repl
  ([connection] (run-repl connection nil))
  ([connection {:keys [prompt] :as options}]
    (let [{:keys [major minor incremental qualifier]} *clojure-version*]
      (loop [ns (execute-with-client connection options "")]
        (prompt ns)
        (flush)
        (let [eof (Object.)
              request-prompt (Object.)
              read-error (Object.)
              [raw-input read-result]
                (try
                  (binding [*ns* (eval-state/get-ns)]
                    (repl-parse request-prompt eof read-error)))]
          (cond (reply.exit/done? eof read-result)
                  nil
                (= request-prompt read-result)
                  (recur ns)
                (= read-error read-result)
                  (do (println raw-input) ; where we stash any read exceptions
                      (recur ns))
                (:remainder read-result)
                  (do (execute-with-client connection (assoc options :interactive true) (apply str raw-input))
                      (recur ns))
                :else
                  (recur (execute-with-client
                           connection
                           (assoc options :interactive true)
                           (apply str raw-input)))))))))

;; TODO: this could be less convoluted if we could break backwards-compat
(defn- url-for [attach host port]
  (if (and attach (re-find #"^\w+://" attach))
    attach
    (let [[port host] (if attach
                        (reverse (.split attach ":"))
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
      (pr-str `(binding [*ns* (symbol ~(deref current-ns))] ~form)))
    (read-string @results)))

(defn main
  "Mostly ripped from nREPL's cmdline namespace."
  [options]
  (let [connection         (get-connection options)
        client             (nrepl/client connection Long/MAX_VALUE)
        session            (nrepl/new-session client)
        completion-session (nrepl/new-session client)]
    (reset! current-session session)
    (swap! response-queues assoc session (ConcurrentLinkedQueue.))
    (swap! response-queues assoc completion-session (ConcurrentLinkedQueue.))
    (let [options (assoc options :prompt
                    (fn [ns]
                      (reader.jline/prepare-for-read
                        (partial completion-eval client completion-session)
                        ns)))
          options (if (:color options)
                    (merge options nrepl.cmdline/colored-output)
                    options)]
      (.start (Thread.
        (fn []
          (when-let [{:keys [out err] :as resp}
                  (nrepl.transport/recv connection 100)]
            (when err (print err))
            (when out (print out))
            (when-not (or err out)
              (.offer (@response-queues (:session resp)) resp))
            (flush))
          (recur))))
      (execute-with-client
               client
               (assoc options :value (constantly nil))
               (pr-str (list 'do
                         (reply.initialization/export-definition
                           'reply.signals/set-signal-handler!)
                         '(set-signal-handler! "INT" (fn [s]))
                         (reply.initialization/construct-init-code options))))

      (handle-client-interruption! client)
      (run-repl client options))))

