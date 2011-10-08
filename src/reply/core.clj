(ns reply.core
  (:use [clojure.main :only [repl repl-read]]
        [clojure.repl :only [set-break-handler!]])
  (:import [scala.tools.jline.console ConsoleReader]
           [scala.tools.jline.console.history FileHistory]
           [java.io File
                    PrintWriter]))

(def input-getter (atom (future)))

(defn handle-ctrl-c [signal]
  (println "^C")
  (future-cancel @input-getter))

(defn jline-read [reader request-prompt request-exit]
  (try
    (let [future-input (future
                         (try
                           (.setPrompt reader (format "%s=> " (ns-name *ns*)))
                           (.readLine reader)
                           (catch Throwable e
                             nil)))
          _ (reset! input-getter future-input)
          input (deref future-input)]
      (cond (not input)
              request-exit
            (every? #(Character/isWhitespace %) input)
              request-prompt
            :else
              (read (java.io.PushbackReader. (java.io.StringReader. input)))))
    (catch Throwable e
      (-> (.getCursorBuffer reader)
          (.clear))
      request-prompt)))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)
    (try
      (let [reader (ConsoleReader.)
            home (System/getProperty "user.home")
            history (FileHistory. (File. home ".jline-reply.history"))]
        (doto reader
          (.setBellEnabled false)
          (.setHistory history))

        (repl :read (partial jline-read reader)
              :prompt (constantly ""))

        (.flush history))

      ; TODO: surely we can avoid this - not sure what's keeping the jvm alive.
      (System/exit 0)

      (catch Exception e
        (prn e))))

