(ns reply.eval-modes.nrepl
  (:require [clojure.tools.nrepl.cmdline :as nrepl.cmdline]
            [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.misc :as nrepl.misc]
            [clojure.tools.nrepl.server :as nrepl.server]
            [clojure.tools.nrepl.transport :as nrepl.transport]
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
        (client {:op "interrupt" :session @current-session :interrupt-id command-id})))))

(defn execute-with-client [client options form]
  (let [command-id (nrepl.misc/uuid)
        response-seq (nrepl/message client {:op "eval" :code form :id command-id :session @current-session})]
    (reset! current-command-id command-id)
    (doall (for [{:keys [ns value out err] :as res}
                   (take-while #(not (some #{"done" "interrupted" "error"} (:status %)))
                               response-seq)]
      (do
        (when value ((:value options print) value))
        (when out ((:out options print) out))
        (when err ((:err options print) err))
        (flush)
        (when ns (reset! current-ns ns)))))
    (reset! current-command-id nil)
    @current-ns))

(defn run-repl
  ([connection] (run-repl connection nil))
  ([connection {:keys [prompt] :as options}]
    (let [{:keys [major minor incremental qualifier]} *clojure-version*]
      (loop [ns "user"]
        (println)
        (prompt ns)
        (flush)
        (let [done (Object.)
              read-result (try (read)
                            (catch Exception e
                              (if (= (.getMessage e) "EOF while reading")
                                done
                                (prn e))))]
          (if (= done read-result) nil
              (recur (execute-with-client
                       connection
                       options
                       (pr-str read-result)))))))))

(defn get-connection [options]
  (let [port (if-let [attach-port (:attach options)]
               (Integer/parseInt attach-port)
               (-> (nrepl.server/start-server :port (Integer/parseInt (or (:port options) "0")))
                   deref
                   :ss
                   .getLocalPort))]
    (nrepl/connect :host (or (:host options) "localhost") :port port)))

(defn adhoc-eval [client form]
  (let [results (atom "nil")]
    (execute-with-client
      client
      {:value (partial reset! results)
       :out print
       :err print}
      (pr-str form))
    (read-string @results)))

(defn main
  "Mostly ripped from nREPL's cmdline namespace."
  [options]
  (let [connection (get-connection options)
        client (nrepl/client connection 10000)
        session (nrepl/new-session client)]
    (reset! current-session session)
    (let [options (assoc options :prompt
                    (fn [ns]
                      (reader.jline/prepare-for-read
                        (partial adhoc-eval client))))
          options (if (:color options)
                    (merge options nrepl.cmdline/colored-output)
                    options)]
      (execute-with-client
               client
               options
               (pr-str (list 'do
                         (reply.initialization/construct-init-code options)
                         (reply.initialization/export-definition 'reply.signals/set-signal-handler!)
                         '(set-signal-handler! "INT" (fn [s]))
                         nil)))

      (handle-client-interruption! client)
      (run-repl client options))))

