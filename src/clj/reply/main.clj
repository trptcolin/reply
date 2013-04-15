(ns reply.main
  (:require [reply.eval-modes.nrepl :as eval-modes.nrepl]
            [reply.eval-modes.standalone :as eval-modes.standalone]
            [reply.exit :as exit]
            [reply.hacks.printing :as hacks.printing]
            [reply.initialization :as initialization]
            [reply.signals :as signals]
            [clojure.main]
            [clojure.repl]
            [clojure.tools.cli :as cli]
            [clj-stacktrace.repl]))

(defn parse-args [args]
  (cli/cli args
           ["-h" "--help" "Show this help screen" :flag true]
           ["-e" "--eval" "--custom-eval"
            "Provide a custom form to evaluate in the user ns"
            :parse-fn read-string]
           ["-i" "--init" "--custom-init"
            "Provide a Clojure file to evaluate in the user ns"
            :parse-fn initialization/formify-file]
           ["--standalone" "Launch standalone mode instead of the default nREPL"
            :flag true]
           ["--color" "Use color; currently only available with nREPL"
            :flag true]
           ["--skip-default-init" "Skip the default initialization code"
            :flag true]
           ["--history-file" "Provide a path for the history file"]
           ["--prompt" "--custom-prompt" "Provide a custom prompt function"
            :parse-fn read-string]
           ["--subsequent-prompt" "Provide a custom subsequent prompt function"
            :parse-fn read-string]
           ["--attach"
            "Attach to an existing nREPL session on this port or host:port"]
           ["--port" "Start new nREPL server on this port"]))

(defn handle-resume [signal]
  (println "Welcome back!"))

(defmacro with-launching-context [options & body]
  `(try
    (signals/set-signal-handler! "CONT" handle-resume)
    (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential
                  clojure.repl/pst clj-stacktrace.repl/pst]
      ~@body)
    ~@(filter identity
              [(when (resolve 'ex-info)
                 `(catch clojure.lang.ExceptionInfo e#
                    (let [status# (:status (:object (ex-data e#)))
                          body# (:body (:object (ex-data e#)))]
                      (cond (= 401 status#) (println "Unauthorized.")
                            (number? status#) (println "Remote error:"
                                                       (slurp ~body))
                            :else (clojure.repl/pst e#)))))
               '(catch Throwable t# (clojure.repl/pst t#))])
    (finally (exit/exit))))

(defn launch-nrepl [options]
  "Launches the nREPL version of REPL-y, with options already parsed out. The
  options map can also include :input-stream and :output-stream entries, which
  must be Java objects passed via this entry point, as they can't be parsed
  from a command line."
  (with-launching-context options
    (eval-modes.nrepl/main options)))

(defn launch-standalone
  "Launches the standalone (non-nREPL) version of REPL-y, with options already
  parsed out"
  [options]
  (with-launching-context options
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
