(ns reply.core
  (:use [clojure.main :only [repl repl-read]]
        [clojure.repl :only [set-break-handler!]])
  (:import [scala.tools.jline.console ConsoleReader]
           [java.io File
                    PrintWriter]))

(def input-getter (atom (future)))

(defn handle-ctrl-c [signal]
  (println "^C")
  (future-cancel @input-getter))

(defn jline-read [request-prompt request-exit]
  (try
    (let [future-input (future
                         (try
                           (let [reader (ConsoleReader.)]
                             (doto reader
                               (.setBellEnabled false)
                               (.setPrompt (format "%s=> " (ns-name *ns*))))
                             (.readLine reader))
                           (catch Throwable e
                             (print "caught throwable in readLine: ")
                             (prn e)
                             nil)))
          _ (reset! input-getter future-input)
          input (deref future-input)
          ]
      (cond (not input)
              request-exit
            (every? #(Character/isWhitespace %) input)
              request-prompt
            :else
              (read (java.io.PushbackReader. (java.io.StringReader. input)))))
    (catch Throwable e
      request-prompt)))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)
    (try

      (repl :read jline-read
            :prompt (constantly ""))

      ; TODO: surely we can avoid this - not sure what's keeping the jvm alive.
      (System/exit 0)

      (catch Throwable e
        (print "wrapping repl call: " )
        (prn e))))

