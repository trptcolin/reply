(ns reply.main
  (:require [reply.eval-modes.nrepl :as eval-modes.nrepl]
            [reply.eval-modes.standalone :as eval-modes.standalone]
            [reply.hacks.printing :as hacks.printing]
            [reply.reader.jline :as reader.jline]
            [reply.signals :as signals]
            [clojure.main]
            [clojure.repl]
            [clj-stacktrace.repl]))

(defn exit
  "Exits the REPL. This is fairly brutal, does (System/exit 0)."
  []
  (shutdown-agents)
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

      "--standalone" (recur (cons arg more) (assoc arg-map :standalone true))
      "--color" (recur (cons arg more) (assoc arg-map :color true))

      "--skip-default-init" (recur (cons arg more)
                                   (assoc arg-map :skip-default-init true))
      arg-map)))

(defn handle-resume [signal]
  (println "Welcome back!")
  (reader.jline/resume-reader))

(defmacro with-launching-context [& body]
  `(try
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(reader.jline/shutdown-reader)))
    (signals/set-signal-handler! "CONT" handle-resume)
    (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential
                  clojure.repl/pst clj-stacktrace.repl/pst]
      ~@body)
    (catch Exception e# (clojure.repl/pst e#))
    (finally (exit))))

(defn launch-nrepl [options]
  "Launches the nREPL version of REPL-y, with options already
  parsed out"
  (with-launching-context
    (reader.jline/with-jline-in
      (eval-modes.nrepl/main options))))

(defn launch-standalone
  "Launches the standalone (non-nREPL) version of REPL-y, with options already
  parsed out"
  [options]
  (with-launching-context
    (eval-modes.standalone/main options)))

(declare -main) ; for --help
(defn launch
  "Entry point for tools which may prefer to send a map of options rather than a
varargs list of arguments.
Available options: [:help :custom-init :skip-default-init :standalone :attach :port :color]
See -main for descriptions."
  [options]
  (cond (:help options) (do (println (clojure.repl/doc -main)) (exit))
        (:standalone options) (launch-standalone options)
        :else (launch-nrepl options)))

(defn -main
  "Launches a REPL. Customizations available:
  -h/--help:           Show this help screen
  -i/--init:           Provide a Clojure file to evaluate in the user ns
  -e/--eval:           Provide a custom form on the command line to evaluate in the user ns
  --skip-default-init: Skip the default initialization code
  --standalone:        Launch standalone mode instead of the default nREPL
  --attach:            Attach to an existing nREPL session on this port or host:port, when used with nREPL
  --port:              Start a new nREPL session on this port, when used with nREPL
  --color:             Use color; currently only available with nREPL"
  [& args]
  (launch (parse-args args)))
