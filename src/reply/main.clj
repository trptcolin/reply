(ns reply.main
  (:use [clojure.main :only [repl]])
  (:require [reply.cancellation :as cancellation]
            [reply.eval-state :as eval-state]
            [reply.hacks.complete :as hacks.complete]
            [reply.hacks.printing :as hacks.printing]
            [reply.reader.jline :as reader.jline]))

(def reply-read
  (fn [prompt exit]
    (cancellation/starting-read!)
    (reader.jline/read prompt exit)))

(def reply-eval
  (cancellation/act-in-future
    (fn [form]
      (reply.eval-state/with-bindings
        (partial eval form)))))

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

(defn help []
  (println "Exit:    Control+D")
  (println "Docs:    (doc function-name-here)")
  (println "         (find-doc \"part-of-name-here\")")
  (println "Javadoc: (javadoc java-object-or-class-here)")
  (println "Source:  (source function-name-here)"))

(defn exit []
  (shutdown-agents)
  (reader.jline/shutdown-reader)
  (println "Bye for now!")
  (System/exit 0))

(defn setup-conveniences []
  (intern 'user 'exit exit)
  (intern 'user 'quit exit)
  (intern 'user 'help help))

(defn -main [& args]
  (set-signal-handler! "INT" handle-ctrl-c)
  (set-signal-handler! "CONT" handle-resume)

  (println "Clojure" (clojure-version))
  (help)
  (setup-conveniences)

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential
                complete/resolve-class hacks.complete/resolve-class]
    (repl :read reply-read
          :eval reply-eval
          :print reply-print
          :prompt (constantly false)
          :need-prompt (constantly false)))

  (exit))

