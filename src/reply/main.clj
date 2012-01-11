(ns reply.main
  (:use [clojure.main :only [repl]]
        [clojure.repl :only [set-break-handler!]])
  (:require [reply.cancellation :as cancellation]
            [reply.hacks.printing :as hacks.printing]
            [reply.reader.jline :as reader.jline]))

(def main-thread (Thread/currentThread))

(def reply-read
  (fn [prompt exit]
    (cancellation/starting-read!)
    (reader.jline/read prompt exit)))

(def reply-eval
  (cancellation/act-in-future eval))

(def reply-print
  (cancellation/act-in-future prn))

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)
  (cancellation/stop-running-actions)
  (reader.jline/reset-reader))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)
  (println "Clojure" (clojure-version))

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential]
    (repl :read reply-read
          :eval reply-eval
          :print reply-print
          :prompt (constantly false)
          :need-prompt (constantly false)))

  (shutdown-agents))

