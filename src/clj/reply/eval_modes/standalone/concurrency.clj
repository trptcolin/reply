(ns reply.eval-modes.standalone.concurrency
  (:require [clojure.main]))

(def actions (atom []))

(defn starting-read! []
  (doall (map #(reset! % nil) @actions)))

(defn act-on-form [action-state act form]
  (swap! action-state assoc :thread (Thread/currentThread))
  (let [result (act form)]
    (swap! action-state assoc :completed true)
    result))

(defn act-in-future [act]
  (let [action-state (atom nil)]
    (swap! actions conj action-state)
    (fn [form]
      (try
        (reset! action-state {})
        @(future (act-on-form action-state act form))
        (catch Throwable e
          (throw e))))))

(defn stop [action & {:keys [hard-kill-allowed]}]
  (let [thread (:thread @action)]
    (when thread (.interrupt thread))
    (when hard-kill-allowed
      (Thread/sleep 2000)
      (when (and @action (not (:completed @action)) (.isAlive thread))
        (println ";;;;;;;;;;")
        (println "; Sorry, have to call Thread.stop on this command.")
        (println ";;;;;;;;;;")
        (.stop thread)))))

(defn stop-running-actions []
  (doall (map #(stop % :hard-kill-allowed true) @actions)))


