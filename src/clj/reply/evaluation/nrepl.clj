(ns reply.evaluation.nrepl
  (:require [clojure.tools.nrepl.cmdline :as nrepl.cmdline]
            [clojure.tools.nrepl :as nrepl]
            [reply.concurrency]
            [reply.initialization]))

(def current-command (atom (fn ([]) ([x]))))

(defn execute-with-connection [port form]
  (let [connection (nrepl/connect "localhost" port)
        response-fn ((:send connection) form)]
    (reset! current-command response-fn)
    (for [{:keys [ns] :as res} (nrepl/response-seq response-fn)]
      (do
        (when (:value res) (print (:value res)))
        (when (:out res) (print (:out res)))
        (when (:err res) (print (:err res)))
        ns))))

(defn run-repl
  ([port] (run-repl port nil))
  ([port {:keys [prompt err out value]
          :or {prompt #(print (str % "=> "))
               err print
               out print
               value print}}]
    (let [connection (nrepl/connect "localhost" port)
          {:keys [major minor incremental qualifier]} *clojure-version*]
      (loop [ns "user"]
        (prompt ns)
        (flush)
        (recur (last (execute-with-connection port (pr-str (read)))))))))

(defn main
  "Ripped directly from nREPL. The only changes are in namespacing, running of
initial code, and options."
  [options]
  (let [[ssocket _] (nrepl/start-server (Integer/parseInt (or (options "--port") "0")))
        local-port (.getLocalPort ssocket)]
    (reply.concurrency/set-signal-handler! "INT" (fn [sig] (println "^C") (@current-command :interrupt)))
    (when-let [ack-port (options "--ack")]
      (binding [*out* *err*]
        (println (format "ack'ing my port %d to other server running on port %s"
                   local-port ack-port)
          (:status (#'clojure.tools.nrepl/send-ack local-port (Integer/parseInt ack-port))))))
    (doall (execute-with-connection
             local-port
             (pr-str (reply.initialization/construct-init-code options))))
    (if (options "--interactive")
      (run-repl local-port
        (if (options "--color")
          nrepl.cmdline/colored-output
          options))
      (Thread/sleep Long/MAX_VALUE))))

