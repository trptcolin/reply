(ns reply.cancellation
  (:use [clojure.main :only [repl-exception]]))

(def evaling-line (atom nil))
(def printing-line (atom nil))

(defn starting-read! []
  (reset! evaling-line nil)
  (reset! printing-line nil))

(defn act-in-future [form action-atom base-action]
  (try
    (reset! action-atom {})
    (let [act-on-form (fn []
                        (swap! action-atom assoc :thread (Thread/currentThread))
                        (let [result (base-action form)]
                          (swap! action-atom assoc :completed true)
                          result))]
      @(future (act-on-form)))
    (catch Throwable e
      (println (repl-exception e))
      (.printStackTrace e))))

(defn stop [action & {:keys [hard-kill-allowed]}]
  (let [thread (:thread @action)]
    (when thread (.interrupt thread))
    (when hard-kill-allowed
      (Thread/sleep 2000)
      (when (and @action (not (:completed @action)) (.isAlive thread))
        (println ";;;;;;;;;;")
        (println "; Sorry, have to call Thread.stop on this command, because it's not dying.")
        (println ";;;;;;;;;;")
        (.stop thread)))))

(defn stop-running-actions []
  (stop printing-line)
  (stop evaling-line :hard-kill-allowed true))

