(ns reply.main
  (:require [reply.eval-modes.nrepl :as eval-modes.nrepl]
            [reply.eval-modes.standalone :as eval-modes.standalone]
            [reply.exit :as exit]
            [reply.hacks.printing :as hacks.printing]
            [reply.initialization :as initialization]
            [reply.reader.jline :as reader.jline]
            [reply.signals :as signals]
            [clojure.main]
            [clojure.repl]
            [clojure.tools.cli :as cli]
            [clj-stacktrace.repl]))

(defn parse-args [args]
  (cli/cli args
           ["-h" "--help" "Show this help screen" :flag true]
           ["-e" "--eval" "--custom-eval" "Provide a custom form on the command line to evaluate in the user ns" :parse-fn read-string]
           ["-i" "--init" "--custom-init" "Provide a Clojure file to evaluate in the user ns" :parse-fn initialization/formify-file]
           ["--standalone" "Launch standalone mode instead of the default nREPL" :flag true]
           ["--color" "Use color; currently only available with nREPL" :flag true]
           ["--skip-default-init" "Skip the default initialization code" :flag true]
           ["--history-file" "Provide a path for the history file"]
           ["--prompt" "--custom-prompt" "Provide a custom prompt function" :parse-fn read-string]
           ["--attach" "Attach to an existing nREPL session on this port or host:port, when used with nREPL"]
           ["--port" "Start new nREPL server on this port"]))

(defn handle-resume [signal]
  (println "Welcome back!")
  (reader.jline/resume-reader))

(defmacro with-launching-context [options & body]
  `(try
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(reader.jline/shutdown-reader)))
    (signals/set-signal-handler! "CONT" handle-resume)
    (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential
                  clojure.repl/pst clj-stacktrace.repl/pst]
      (reader.jline/setup-reader! ~options)
      ~@body)
    (catch clojure.lang.ExceptionInfo e#
      (let [status# (:status (:object (ex-data e#)))
            body# (:body (:object (ex-data e#)))]
        (cond (= 401 status#) (println "Unauthorized.")
              (number? status#) (println "Remote error:" (slurp body#))
              :else (clojure.repl/pst e#))))
    (catch Throwable t# (clojure.repl/pst t#))
    (finally (exit/exit))))

(defn set-prompt [options]
  (when-let [prompt-form (:custom-prompt options)]
    (reader.jline/set-prompt-fn! (eval prompt-form))))

(defn launch-nrepl [options]
  "Launches the nREPL version of REPL-y, with options already
  parsed out"
  (with-launching-context options
    (reader.jline/with-jline-in
      (set-prompt options)
      (eval-modes.nrepl/main options))))

(defn launch-standalone
  "Launches the standalone (non-nREPL) version of REPL-y, with options already
  parsed out"
  [options]
  (with-launching-context options
    (set-prompt options)
    (eval-modes.standalone/main options)))

(defn launch
  "Entry point for tools which may prefer to send a map of options
  rather than a varargs list of arguments. See parse-args for available
  options."
  [options]
  (if (:standalone options)
    (launch-standalone options)
    (launch-nrepl options)))

(defn -main [& args]
  (let [[options args banner]
          (try (parse-args args)
            (catch Exception e
              (parse-args ["--help"])))]
    (when (:help options)
      (println banner)
      (exit/exit))
    (launch options)))
