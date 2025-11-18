(ns reply.eval-modes.standalone
  (:require [reply.conversions :refer [->fn]]
            [reply.eval-modes.shared :as eval-modes.shared]
            [reply.eval-modes.standalone.concurrency :as concurrency]
            [reply.eval-state :as eval-state]
            [reply.exit :as exit]
            [reply.initialization :as initialization]
            [reply.reader.simple-jline :as simple-jline]
            [reply.signals :as signals]))

(defn make-future-read-eval [_options]
  (concurrency/act-in-future
    (fn [form]
      (eval-state/with-bindings
        (fn [] (eval (read-string form)))))))

(defn make-reply-read-eval [options]
  (let [future-read-eval (make-future-read-eval options)]
    (fn [form]
      (future-read-eval form))))

(defn handle-ctrl-c [_signal]
  (concurrency/stop-running-actions))

(defn execute [{:keys [value-to-string print-value print-out print-err] :as options}
               form]
  (let [reply-read-eval (make-reply-read-eval options)]
    (when-not (empty? form)
      (try
        (print-value (value-to-string (reply-read-eval form)))
        (catch InterruptedException e nil)
        (catch Throwable t
          (let [e (clojure.main/repl-exception t)]
            ((or print-err print) e))))
      (when (:interactive options) (println)))
    (eval-state/get-ns-string)))

(defn completion-fn [form]
  (binding [*print-length* nil]
    ((concurrency/act-in-future eval) form)))

(defn run-repl [{:keys [prompt subsequent-prompt history-file
                        input-stream output-stream read-line-fn]
                 :as options}]
  (loop [ns (eval-state/get-ns-string)]
    (let [eof (Object.)
          execute (partial execute (assoc options :interactive true))
          pf-opts {:request-exit eof
                   :prompt-string (prompt ns)
                   :ns ns
                   :read-line-fn read-line-fn
                   :history-file history-file
                   :input-stream input-stream
                   :output-stream output-stream
                   :completion-eval-fn completion-fn
                   :subsequent-prompt-string (subsequent-prompt ns)
                   :text-so-far nil}
          parsed-forms-fn (eval-modes.shared/load-parsed-forms-fn-in-background)
          read-text (read-line-fn pf-opts)
          forms (@parsed-forms-fn read-text pf-opts)]
      (if (exit/done? eof (first forms))
        nil
        (recur (last (doall (map execute forms))))))))

(defn main [options]
  (signals/set-signal-handler! "INT" handle-ctrl-c)
  (eval-state/set-ns "user")
  (let [options (eval-modes.shared/set-default-options options)
        options (-> options
                    (assoc :caught (->fn (:caught options)
                                         clojure.main/repl-caught)
                           :value-to-string (->fn (:value-to-string options)
                                                  pr-str)
                           :reader (simple-jline/setup-reader options)))
        non-interactive-eval (fn [form]
                               (execute (assoc options :print-value (constantly nil))
                                        (binding [*print-length* nil
                                                  *print-level* nil]
                                          (pr-str form))))]
    (non-interactive-eval (initialization/construct-init-code options))
    (run-repl options)))

