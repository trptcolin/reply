(ns reply.main
  (:use [clojure.main :only [repl]]
        [clojure.repl :only [set-break-handler!]])
  (:require [reply.cancellation :as cancellation]
            [reply.hacks.printing :as hacks.printing]
            [reply.reader.jline :as reader.jline]
            [clojure.string :as str]))

(def main-thread (Thread/currentThread))

(defn reply-eval [form]
  (cancellation/act-in-future form cancellation/evaling-line eval))

(defn reply-print [form]
  (cancellation/act-in-future form cancellation/printing-line prn))

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)
  (cancellation/stop-running-actions)
  (reader.jline/reset-reader))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)
  (println "Clojure" (clojure-version))

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential]
    (repl :read (fn [prompt exit]
                  (cancellation/starting-read!)
                  (reader.jline/read prompt exit))
          :eval reply-eval
          :print reply-print
          :prompt (constantly false)
          :need-prompt (constantly false)))

  (shutdown-agents))

