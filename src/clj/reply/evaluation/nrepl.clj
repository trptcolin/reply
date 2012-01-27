(ns reply.evaluation.nrepl
  (:require [clojure.tools.nrepl.cmdline :as nrepl.cmdline]
            [clojure.tools.nrepl :as nrepl]
            [reply.initialization]))

(defn execute-with-connection [port form]
  (let [connection (nrepl/connect "localhost" port)]
    (for [{:keys [ns] :as res} (nrepl/response-seq ((:send connection) form))]
      (do
        (when (:value res) (print (:value res)))
        (when (:out res) (print (:out res)))
        (when (:err res) (print (:err res)))
        ns))))

(defn main
  "Ripped directly from nREPL. The only changes are in namespacing, running of
initial code, and options."
  [options]
  (let [[ssocket _] (nrepl/start-server (Integer/parseInt (or (options "--port") "0")))]
    (when-let [ack-port (options "--ack")]
      (binding [*out* *err*]
        (println (format "ack'ing my port %d to other server running on port %s"
                   (.getLocalPort ssocket) ack-port)
          (:status (#'clojure.tools.nrepl/send-ack (.getLocalPort ssocket) (Integer/parseInt ack-port))))))
    (doall (execute-with-connection
             (.getLocalPort ssocket)
             (pr-str (reply.initialization/construct-init-code options))))
    (if (options "--interactive")
      (#'nrepl.cmdline/run-repl (.getLocalPort ssocket)
        (if (options "--color")
          nrepl.cmdline/colored-output
          options))
      (Thread/sleep Long/MAX_VALUE))))

