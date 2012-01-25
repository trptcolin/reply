(ns reply.main
  (:require [reply.concurrency :as concurrency]
            [reply.eval-state :as eval-state]
            [reply.hacks.complete :as hacks.complete]
            [reply.hacks.printing :as hacks.printing]
            [reply.reader.jline :as reader.jline]
            [clojure.main]
            [clojure.repl]
            [clj-stacktrace.repl]))

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

(defn help
  "Prints a list of helpful commands."
  []
  (println "    Exit: Control+D or (exit) or (quit)")
  (println "Commands: (help)")
  (println "    Docs: (doc function-name-here)")
  (println "          (find-doc \"part-of-name-here\")")
  (println "  Source: (source function-name-here)")
  (println " Javadoc: (javadoc java-object-or-class-here)")
  (println "Examples from clojuredocs.org:")
  (println "          (clojuredocs name-here)")
  (println "          (clojuredocs \"ns-here\" \"name-here\")"))

(defn exit
  "Exits the REPL."
  []
  (shutdown-agents)
  (reader.jline/shutdown-reader)
  (println "Bye for now!")
  (System/exit 0))

(defn intern-with-meta [ns sym value-var]
  (intern ns
          (with-meta sym (assoc (meta value-var) :ns (the-ns ns)))
          @value-var))

(def default-init-code
  '(do
    (println "Welcome to REPL-y!")
    (println "Clojure" (clojure-version))
    (use '[clojure.repl :only (source apropos dir pst doc find-doc)])
    (use '[clojure.java.javadoc :only (javadoc)])
    (use '[clojure.pprint :only (pp pprint)])
    (require '[cd-client.core])
    (def exit reply.main/exit)
    (def quit reply.main/exit)
    (def help reply.main/help)
    (help)
    (reply.main/intern-with-meta 'user 'clojuredocs #'cd-client.core/pr-examples)))

(defn setup-conveniences
  ([& {:keys [skip-default-init custom-init] :as options}]
    (in-ns 'user)
    (when-not skip-default-init (eval default-init-code))
    (when custom-init (eval custom-init))
    (in-ns 'reply.main)))

(defn launch [args]
  (set-signal-handler! "INT" handle-ctrl-c)
  (set-signal-handler! "CONT" handle-resume)

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential
                complete/resolve-class hacks.complete/resolve-class
                clojure.repl/pst clj-stacktrace.repl/pst]
    (clojure.main/repl :read reply-read
          :eval reply-eval
          :print reply-print
          :init #(setup-conveniences :skip-default-init false :custom-init '())
          :prompt (constantly false)
          :need-prompt (constantly false)))

  (exit))

