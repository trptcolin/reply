(ns reply.eval-modes.standalone
  (:require [reply.eval-modes.standalone.concurrency :as concurrency]
            [reply.eval-state :as eval-state]
            [reply.exit :as exit]
            [reply.initialization :as initialization]
            [reply.signals :as signals]))

(def reply-read
  (fn [prompt exit]
    (concurrency/starting-read!)
    (binding [*ns* (eval-state/get-ns)]
      (print (str (ns-name *ns*) "=> "))
      (flush)
      (let [read-result (clojure.main/repl-read prompt exit)]
        (if (exit/done? exit read-result)
          exit
          read-result)))))

(def reply-eval
  (concurrency/act-in-future
    (fn [form]
      (eval-state/with-bindings
        (partial eval form)))))

(def reply-print
  (concurrency/act-in-future prn))

(defn handle-ctrl-c [signal]
  (concurrency/stop-running-actions))

(defn main [options]
  (signals/set-signal-handler! "INT" handle-ctrl-c)
  (clojure.main/repl :read reply-read
                     :eval reply-eval
                     :print reply-print
                     :init #(initialization/eval-in-user-ns
                             (initialization/construct-init-code options))
                     :prompt (constantly false)
                     :need-prompt (constantly false)))

