(ns reply.eval-modes.standalone
  (:require [reply.conversions :refer [->fn]]
            [reply.eval-modes.shared :as eval-modes.shared]
            [reply.eval-modes.standalone.concurrency :as concurrency]
            [reply.eval-state :as eval-state]
            [reply.exit :as exit]
            [reply.initialization :as initialization]
            [reply.parsing :as parsing]
            [reply.reader.simple-jline :as simple-jline]
            [reply.signals :as signals]))

(defn make-future-eval [options]
  (concurrency/act-in-future
    (fn [form]
      (eval-state/with-bindings
        (fn [] (eval form))))))

(defn make-reply-eval [options]
  (let [future-eval (make-future-eval options)]
    (fn [form]
      (simple-jline/shutdown)
      (future-eval form))))

(defn handle-ctrl-c [signal]
  (concurrency/stop-running-actions))

(defn execute [{:keys [value-to-string print-value print-out print-err] :as options}
               form]
  (let [actual-form (try (read-string form)
                         (catch Throwable t ""))
        reply-eval (make-reply-eval options)
        failure-sentinel (Object.)
        result (try (reply-eval actual-form)
                 (catch InterruptedException e nil)
                 (catch Throwable t
                   (let [e (clojure.main/repl-exception t)]
                     ((or print-err print) e)
                     (println))
                   failure-sentinel))]
    ;lazyseq: (1 2 3 4 5 ...)
    ;pr: "(1 2 3 4 5 ...)"
    (when (not= failure-sentinel result)
      (print-value (value-to-string result))
      (when (:interactive options) (println)))
    (eval-state/get-ns-string)))

(defn run-repl [{:keys [prompt subsequent-prompt history-file
                        input-stream output-stream read-line-fn]
                 :as options}]
  (loop [ns (eval-state/get-ns-string)]
    (let [eof (Object.)
          execute (partial execute (assoc options :interactive true))
          forms (parsing/parsed-forms
                  {:request-exit eof
                   :prompt-string (prompt ns)
                   :ns ns
                   :read-line-fn read-line-fn
                   :history-file history-file
                   :input-stream input-stream
                   :output-stream output-stream
                   :subsequent-prompt-string (subsequent-prompt ns)
                   :text-so-far nil})]
      (if (exit/done? eof (first forms))
        nil
        (recur (last (doall (map execute forms))))))))

(defn main [options]
  (signals/set-signal-handler! "INT" handle-ctrl-c)
  (eval-state/set-ns "user")
  (let [options (eval-modes.shared/set-default-options options)
        options (assoc options :caught (->fn (:caught options)
                                             clojure.main/repl-caught))
        options (assoc options :value-to-string (->fn (:value-to-string options)
                                                      pr-str))
        options (assoc options
                       :read-line-fn
                       (partial
                         simple-jline/safe-read-line
                         (fn [form]
                           (binding [*print-length* nil]
                             ((make-future-eval options) form)))))
        non-interactive-eval (fn [form]
                               (execute (assoc options :print-value (constantly nil))
                                        (binding [*print-length* nil
                                                  *print-level* nil]
                                          (pr-str form))))]
    (non-interactive-eval (initialization/construct-init-code options))
    (run-repl options)
    (simple-jline/shutdown)))

