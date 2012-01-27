(ns reply.main
  (:require [reply.concurrency :as concurrency]
            [reply.eval-state :as eval-state]
            [reply.evaluation.nrepl :as evaluation.nrepl]
            [reply.hacks.complete :as hacks.complete]
            [reply.hacks.printing :as hacks.printing]
            [reply.initialization :as initialization]
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

(defn exit
  "Exits the REPL."
  []
  (shutdown-agents)
  (reader.jline/shutdown-reader)
  (println "Bye for now!")
  (System/exit 0))

(defn parse-args [args]
  (loop [[option arg & more :as args] args
         arg-map {:custom-init '()}]
    (case option
      "-i" (recur more (assoc arg-map :custom-init (read-string arg)))
      "--init" (recur more (assoc arg-map :custom-init (read-string arg)))
      "--nrepl" (recur (cons arg more)
                       (assoc arg-map :nrepl true))
      "--skip-default-init" (recur (cons arg more)
                                   (assoc arg-map :skip-default-init true))
      arg-map)))

(defn launch-nrepl [options]
  (reader.jline/with-jline-in
    (evaluation.nrepl/main
      (assoc options
        :prompt (fn [ns]
                  (binding [*ns* (symbol ns)]
                    (reply.eval-state/set-bindings!))
                  (reader.jline/prepare-for-read))
        "--interactive" true))))

(defn launch-standalone [options]
  (set-signal-handler! "INT" handle-ctrl-c)
  (set-signal-handler! "CONT" handle-resume)
  (clojure.main/repl :read reply-read
        :eval reply-eval
        :print reply-print
        :init #(initialization/eval-in-user-ns
                (initialization/construct-init-code options))
        :prompt (constantly false)
        :need-prompt (constantly false)))

(defn launch
  "Launches a REPL. Customizations available:
  -i: provide code to evaluate in the user ns
  --skip-default-init: skip the default initialization code
  --nrepl: hook up to nREPL (clojure.tools.nrepl)"
  [& args]
  (try
    (let [arg-map (parse-args args)]
      (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential
                    complete/resolve-class hacks.complete/resolve-class
                    clojure.repl/pst clj-stacktrace.repl/pst]
        (if (:nrepl arg-map)
          (launch-nrepl arg-map)
          (launch-standalone arg-map))))
    (catch Throwable e
      (when (not= (.getMessage e) "EOF while reading")
        (println "Oh noez!")
        (clj-stacktrace.repl/pst e))))

  (exit))

