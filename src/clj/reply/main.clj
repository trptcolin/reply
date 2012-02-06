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

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)
  (concurrency/stop-running-actions)
  (reader.jline/reset-reader))

(defn handle-resume [signal]
  (println "Welcome back!")
  (reader.jline/resume-reader))

(defn exit
  "Exits the REPL. This is fairly brutal, does (System/exit 0)."
  []
  (shutdown-agents)
  (reader.jline/shutdown-reader)
  (println "Bye for now!")
  (System/exit 0))

(defn parse-args [args]
  (loop [[option arg & more :as args] args
         arg-map {:custom-init '()}]
    (case option
      "-e" (recur more (assoc arg-map :custom-init (read-string arg)))
      "--eval" (recur more (assoc arg-map :custom-init (read-string arg)))

      "-i" (recur more (assoc arg-map :custom-init (read-string (slurp arg))))
      "--init" (recur more (assoc arg-map :custom-init (read-string (slurp arg))))

      "--attach" (recur more (assoc arg-map :attach arg))
      "--port" (recur more (assoc arg-map :port arg))

      "-h" (recur (cons arg more) (assoc arg-map :help true))
      "--help" (recur (cons arg more) (assoc arg-map :help true))

      "--nrepl" (recur (cons arg more) (assoc arg-map :nrepl true))
      "--color" (recur (cons arg more) (assoc arg-map :color true))

      "--skip-default-init" (recur (cons arg more)
                                   (assoc arg-map :skip-default-init true))
      arg-map)))

(defn launch-nrepl [options]
  "Launches the nREPL version of REPL-y, with options already
  parsed out"
  (reader.jline/with-jline-in
    (evaluation.nrepl/main
      (assoc options
        :prompt (fn [ns]
                  (binding [*ns* (symbol ns)]
                    (reply.eval-state/set-bindings!))
                  (reader.jline/prepare-for-read))))))

(defn launch-standalone
  "Launches the streamed (non-nREPL) version of REPL-y, with options already
  parsed out"
  [options]
  (concurrency/set-signal-handler! "INT" handle-ctrl-c)
  (clojure.main/repl :read reply-read
        :eval reply-eval
        :print reply-print
        :init #(initialization/eval-in-user-ns
                (initialization/construct-init-code options))
        :prompt (constantly false)
        :need-prompt (constantly false)))

(declare -main) ; for --help
(defn launch
  "Entry point for tools which may prefer to send a map of options rather than a
varargs list of arguments.
Available options: [:help :custom-init :skip-default-init :nrepl :attach :port :color]
See -main for descriptions."
  [options]
  (try
    (concurrency/set-signal-handler! "CONT" handle-resume)
    (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential
                  complete/resolve-class hacks.complete/resolve-class
                  clojure.repl/pst clj-stacktrace.repl/pst]
      (cond (:help options) (println (clojure.repl/doc -main))
            (:nrepl options) (launch-nrepl options)
            :else (launch-standalone options)))
  (catch Exception e (clojure.repl/pst e))
  (finally (exit))))

(defn -main
  "Launches a REPL. Customizations available:
  -h/--help:           Show this help screen
  -i/--init:           Provide a Clojure file to evaluate in the user ns
  -e/--eval:           Provide custom code to evaluate in the user ns
  --skip-default-init: Skip the default initialization code
  --nrepl:             Launch nREPL (clojure.tools.nrepl) in interactive mode
  --attach:            Attach to an existing nrepl session on this port, when used with --nrepl
  --port:              Start a new nrepl session on this port, when used with --nrepl
  --color:             Use color; currently only available with --nrepl"
  [& args]
  (launch (parse-args args)))
