(ns reply.core
  (:use [clojure.main :only [repl repl-read]]
        [clojure.repl :only [set-break-handler!]])
  (:require [reply.printing])
  (:import [scala.tools.jline.console ConsoleReader]
           [scala.tools.jline.console.history FileHistory]
           [java.io File]))

; TODO: allow multi-line input
(defn actual-read [input request-prompt request-exit]
  (binding [*in* (clojure.lang.LineNumberingPushbackReader. (java.io.StringReader. input))]
    (clojure.main/repl-read request-prompt request-exit)))

(def main-thread (Thread/currentThread))

(defn make-reader []
  (let [reader (ConsoleReader.)
        home (System/getProperty "user.home")
        history (FileHistory. (File. home ".jline-reply.history"))]
    (doto reader
      (.setBellEnabled false)
      (.setHistory history))))

(def jline-reader (make-reader))

(defn handle-ctrl-c [signal]
  (println "^C")
  (.interrupt main-thread)
  (-> (.getCursorBuffer jline-reader)
      (.clear)))

(defn jline-read [request-prompt request-exit]
  (try
    (.setPrompt jline-reader (format "%s=> " (ns-name *ns*)))
    (let [input (.readLine jline-reader)]
      (cond (not input)
              request-exit
            (every? #(Character/isWhitespace %) input)
              request-prompt
            :else
              (do
                (Thread/interrupted) ; just to clear the status
                (actual-read input request-prompt request-exit))))
    (catch InterruptedException e
      (-> (.getCursorBuffer jline-reader)
          (.clear))
      (request-prompt))))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)

  (repl :read jline-read
        :prompt (constantly ""))

  (.flush (.getHistory jline-reader))

  (shutdown-agents))


