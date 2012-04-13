(ns reply.eval-modes.nrepl
  (:require [clojure.tools.nrepl.cmdline :as nrepl.cmdline]
            [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.misc :as nrepl.misc]
            [clojure.tools.nrepl.server :as nrepl.server]
            [clojure.tools.nrepl.transport :as nrepl.transport]
            [reply.exit]
            [reply.initialization]
            [reply.reader.jline :as reader.jline]
            [reply.signals :as signals]))

(def current-command-id (atom nil))
(def current-session (atom nil))
(def current-ns (atom (str *ns*)))

(defn handle-client-interruption! [client]
  (signals/set-signal-handler!
    "INT"
    (fn [sig] (print "^C") (flush)
      (when-let [command-id @current-command-id]
        (client {:op "interrupt"
                 :session @current-session
                 :interrupt-id command-id})))))

(defn execute-with-client [client options form]
  (let [command-id (nrepl.misc/uuid)
        session (or (:session options) @current-session)
        session-sender (nrepl/client-session client :session session)
        response-seq (session-sender {:op "eval" :code form :id command-id})]
    (reset! current-command-id command-id)
    (doall
      (for [{:keys [ns value out err] :as res}
              (#'nrepl/take-until
                #(and (= command-id (:id %))
                      (some #{"done" "interrupted" "error"} (:status %)))
                response-seq)]
        (do
          (when (some #{"need-input"} (:status res))
            (.readLine *in*) ; pop off leftover newline from pushback
            (let [input-result (.readLine *in*)]
              (session-sender
                {:op "stdin" :stdin (str input-result "\n")
                 :id (nrepl.misc/uuid)})))
          (when value ((:value options print) value))
          (when out ((:out options print) out))
          (when err ((:err options print) err))
          (flush)
          (when (and ns (not (:session options)))
            (reset! current-ns ns)))))
    (reset! current-command-id nil)
    @current-ns))

(defn run-repl
  ([connection] (run-repl connection nil))
  ([connection {:keys [prompt] :as options}]
    (let [{:keys [major minor incremental qualifier]} *clojure-version*]
      (loop [ns (execute-with-client connection options "")]
        (println)
        (prompt ns)
        (flush)
        (let [eof (Object.)
              read-result (try (read)
                            (catch Exception e
                              (if (= (.getMessage e) "EOF while reading")
                                eof
                                (prn e))))]
          (if (reply.exit/done? eof read-result)
              nil
              (recur (execute-with-client
                       connection
                       options
                       (binding [*print-meta* true]
                         (pr-str read-result))))))))))

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
    (require '[cemerick.drawbridge])
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
       :out print
       :err (constantly nil)
       :session session}
      (pr-str `(binding [*ns* (symbol ~(deref current-ns))] ~form)))
    (read-string @results)))

(defn main
  "Mostly ripped from nREPL's cmdline namespace."
  [options]
  (let [connection         (get-connection options)
        client             (nrepl/client connection Long/MAX_VALUE)
        session            (nrepl/new-session client)
        completion-session (nrepl/new-session client) ]
    (reset! current-session session)
    (let [options (assoc options :prompt
                    (fn [ns]
                      (reader.jline/prepare-for-read
                        (partial completion-eval client completion-session)
                        ns)))
          options (if (:color options)
                    (merge options nrepl.cmdline/colored-output)
                    options)]
      (execute-with-client
               client
               options
               (pr-str (list 'do
                         (reply.initialization/export-definition
                           'reply.signals/set-signal-handler!)
                         '(set-signal-handler! "INT" (fn [s]))
                         (reply.initialization/construct-init-code options)
                         nil)))

      (handle-client-interruption! client)
      (run-repl client options))))

