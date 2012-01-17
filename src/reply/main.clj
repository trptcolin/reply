(ns reply.main
  (:use [clojure.main :only [repl]])
  (:require [reply.cancellation :as cancellation]
            [reply.hacks.printing :as hacks.printing]
            [reply.reader.jline :as reader.jline]))

(def reply-read
  (fn [prompt exit]
    (cancellation/starting-read!)
    (reader.jline/read prompt exit)))

(def reply-eval
  (cancellation/act-in-future eval))

(def reply-print
  (cancellation/act-in-future prn))

(defn set-signal-handler! [signal f]
  (sun.misc.Signal/handle
    (sun.misc.Signal. signal)
    (proxy [sun.misc.SignalHandler] []
      (handle [signal] (f signal)))))

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)
  (cancellation/stop-running-actions)
  (reader.jline/reset-reader))

(defn handle-resume [signal]
  (println "Welcome back!")
  (reader.jline/resume-reader))

(defn -main [& args]
  (set-signal-handler! "INT" handle-ctrl-c)
  (set-signal-handler! "CONT" handle-resume)

  (println "Clojure" (clojure-version))

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential]
    (repl :read reply-read
          :eval reply-eval
          :print reply-print
          :prompt (constantly false)
          :need-prompt (constantly false)))

  (shutdown-agents)
  (reader.jline/shutdown-reader))

