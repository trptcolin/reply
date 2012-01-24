(ns reply.main
  (:require [reply.concurrency :as concurrency]
            [reply.eval-state :as eval-state]
            [reply.hacks.complete :as hacks.complete]
            [reply.hacks.printing :as hacks.printing]
            [reply.reader.jline :as reader.jline]
            [clojure.main]
            [clojure.repl]
            [clj-stacktrace.repl]
            [cd-client.core :as cd]))

(def reply-read
  (fn [prompt exit]
    (concurrency/starting-read!)
    (reader.jline/read prompt exit)))

(def reply-eval
  (concurrency/act-in-future
    (fn [form]
      (eval-state/with-bindings
        (partial eval form)))))

(def reply-print
  (concurrency/act-in-future prn))

(defn set-signal-handler! [signal f]
  (sun.misc.Signal/handle
    (sun.misc.Signal. signal)
    (proxy [sun.misc.SignalHandler] []
      (handle [signal] (f signal)))))

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)
  (concurrency/stop-running-actions)
  (reader.jline/reset-reader))

(defn handle-resume [signal]
  (println "Welcome back!")
  (reader.jline/resume-reader))

(defn help []
  (println "    Exit: Control+D or (exit) or (quit)")
  (println "Commands: (help)")
  (println "    Docs: (doc function-name-here)")
  (println "          (find-doc \"part-of-name-here\")")
  (println "  Source: (source function-name-here)")
  (println " Javadoc: (javadoc java-object-or-class-here)")
  (println "Examples from clojuredocs.org:")
  (println "          (clojuredocs name-here)")
  (println "          (clojuredocs \"ns-here\" \"name-here\")"))

(defn exit []
  (shutdown-agents)
  (reader.jline/shutdown-reader)
  (println "Bye for now!")
  (System/exit 0))

(defn intern-with-meta [ns sym value-var]
  (intern ns
          (with-meta sym (assoc (meta value-var) :ns (the-ns ns)))
          @value-var))

(defn setup-conveniences []
  (intern 'user 'exit exit)
  (intern 'user 'quit exit)
  (intern 'user 'help help)
  (intern-with-meta 'user 'clojuredocs #'cd/pr-examples))

(defn launch [args]
  (set-signal-handler! "INT" handle-ctrl-c)
  (set-signal-handler! "CONT" handle-resume)

  (println "Welcome to REPL-y!")
  (println "Clojure" (clojure-version))
  (help)

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential
                complete/resolve-class hacks.complete/resolve-class
                clojure.repl/pst clj-stacktrace.repl/pst]
    (clojure.main/repl :read reply-read
          :eval reply-eval
          :print reply-print
          :init setup-conveniences
          :prompt (constantly false)
          :need-prompt (constantly false)))

  (exit))

