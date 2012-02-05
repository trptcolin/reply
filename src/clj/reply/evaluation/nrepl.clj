(ns reply.evaluation.nrepl
  (:require [clojure.tools.nrepl.cmdline :as nrepl.cmdline]
            [clojure.tools.nrepl :as nrepl]
            [reply.concurrency]
            [reply.eval-state]
            [reply.initialization]))

(def current-command (atom (fn ([]) ([x]))))

(defn execute-with-connection [connection options form]
  (let [response-fn ((:send connection) form)]
    (reset! current-command response-fn)
    (for [{:keys [ns value out err] :as res} (nrepl/response-seq response-fn)]
      (do
        (when value ((:value options print) value))
        (when out ((:out options print) out))
        (when err ((:err options print) err))
        (flush)
        ns))))

(defn run-repl
  ([connection] (run-repl connection nil))
  ([connection {:keys [prompt]
                :or {prompt #(print (str % "=> "))}
                :as options}]
    (let [{:keys [major minor incremental qualifier]} *clojure-version*]
      (loop [ns "user"]
        (prompt ns)
        (flush)
        (let [done (Object.)
              read-result (try (read)
                            (catch Exception e
                              (if (= (.getMessage e) "EOF while reading")
                                done
                                (prn e))))]
          (if (= done read-result) nil
              (recur (last (execute-with-connection
                       connection
                       options
                       (pr-str read-result))))))))))

(defn get-connection [options]
  (let [port (if-let [attach-port (:attach options)]
               (Integer/parseInt attach-port)
               (let [[ssocket _] (nrepl/start-server (Integer/parseInt (or (:port options) "0")))]
                 (.getLocalPort ssocket)))]
    (nrepl/connect "localhost" port)))

(defn main
  "Ripped from nREPL. The only changes are in namespacing, running of initial
code, and options."
  [options]
  (let [connection (get-connection options)]
    (reply.concurrency/set-signal-handler! "INT" (fn [sig] (println "^C") (@current-command :interrupt)))
    (let [options (if (:color options)
                    (merge options nrepl.cmdline/colored-output)
                    options)]
      (doall (execute-with-connection
               connection
               options
               (pr-str (reply.initialization/construct-init-code options))))
      (run-repl connection options))))

