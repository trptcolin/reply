(ns reply.core
  (:use [clojure.main :only [repl]])
  (:import [scala.tools.jline.console ConsoleReader]
           [java.io PrintWriter]))

(defn -main [& args]
  (let [reader (doto (ConsoleReader.)
                 (.setBellEnabled false)
                 (.setPrompt ""))
        out (-> reader
                (.getOutput)
                (PrintWriter.))
        jline-read (fn [request-prompt request-exit]
                     (let [input (.readLine reader)]
                       (cond (not input)
                               request-exit
                             (every? #(Character/isWhitespace %) input)
                               request-prompt
                             :else
                               (read-string input))))
        jline-prompt (fn [] (.setPrompt reader
                                        (format "%s=> " (ns-name *ns*))))]
    (binding [*out* out]
      (repl :read jline-read
            :prompt jline-prompt))))
