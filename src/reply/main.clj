(ns reply.main
  (:use [clojure.main :only [repl repl-exception]]
        [clojure.repl :only [set-break-handler!]])
  (:require [reply.hacks.printing :as hacks.printing]
            [reply.reader.jline :as reader.jline]
            [clojure.string :as str]))

(def main-thread (Thread/currentThread))

(def evaling-line (atom nil))
(def printing-line (atom nil))

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

(defn reply-eval [form]
  (act-in-future form evaling-line eval))

(defn reply-print [form]
  (act-in-future form printing-line prn))

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)

  (stop printing-line)
  (stop evaling-line :hard-kill-allowed true)

  (reader.jline/reset-reader))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)
;  (setup-reader!)
  (println "Clojure" (clojure-version))

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential]
    (repl :read (fn [prompt exit]
                  (reset! evaling-line nil)
                  (reset! printing-line nil)
                  (reader.jline/read prompt exit))
          :eval reply-eval
          :print reply-print
          :prompt (fn [] false)
          :need-prompt (fn [] false)))

  (shutdown-agents))

