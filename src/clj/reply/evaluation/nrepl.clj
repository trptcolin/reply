(ns reply.evaluation.nrepl
  (:require [clojure.tools.nrepl.cmdline :as nrepl.cmdline]
            [clojure.tools.nrepl :as nrepl]
            [reply.concurrency]
            [reply.eval-state]
            [reply.initialization]))

(def current-command (atom (fn ([]) ([x]))))

(defn execute-with-connection [connection form]
  (let [response-fn ((:send connection) form)]
    (reset! current-command response-fn)
    (for [{:keys [ns value out err] :as res} (nrepl/response-seq response-fn)]
      (do
        (when value (print value))
        (when out (print out))
        (when err (print err))
        (flush)
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
        (recur (last (execute-with-connection connection (pr-str (read)))))))))

(defn main
  "Ripped directly from nREPL. The only changes are in namespacing, running of
initial code, and options."
  [options]
  (let [[ssocket _] (nrepl/start-server (Integer/parseInt (or (options "--port") "0")))
        local-port (.getLocalPort ssocket)
        connection (nrepl/connect "localhost" local-port)]
    (reply.concurrency/set-signal-handler! "INT" (fn [sig] (println "^C") (@current-command :interrupt)))
    (when-let [ack-port (options "--ack")]
      (binding [*out* *err*]
        (println (format "ack'ing my port %d to other server running on port %s"
                   local-port ack-port)
          (:status (#'clojure.tools.nrepl/send-ack local-port (Integer/parseInt ack-port))))))
    (doall (execute-with-connection
             connection
             (pr-str (reply.initialization/construct-init-code options))))
    (if (options "--interactive")
      (run-repl local-port
        (if (options "--color")
          nrepl.cmdline/colored-output
          options))
      (Thread/sleep Long/MAX_VALUE))))

