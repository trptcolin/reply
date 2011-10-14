(ns reply.core
  (:use [clojure.main :only [repl repl-read repl-exception]]
        [clojure.repl :only [set-break-handler!]])
  (:require [reply.printing])
  (:import [reply JlineInputReader]
           [scala.tools.jline.console ConsoleReader]
           [scala.tools.jline.console.history FileHistory]
           [java.io File]))

(defn actual-read [input-reader request-prompt request-exit]
  (binding [*in* input-reader]
    (repl-read request-prompt request-exit)))

(def main-thread (Thread/currentThread))

(defn make-reader []
  (let [reader (ConsoleReader.)
        home (System/getProperty "user.home")
        history (FileHistory. (File. home ".jline-reply.history"))]
    (doto reader
      (.setBellEnabled false)
      (.setHistory history))))

(def jline-reader (atom nil))

(defn get-prompt [ns]
  (format "%s=> " (ns-name ns)))

(defn clear-jline-buffer []
  (-> (.getCursorBuffer @jline-reader)
      (.clear)))

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)
  (.interrupt main-thread)
  (clear-jline-buffer))

(defn set-empty-prompt []
  (.setPrompt
    @jline-reader
    (apply str
           (concat (repeat (- (count (get-prompt *ns*)) 2) \space)
                   [\| \space]))))

(defn jline-read [request-prompt request-exit]
  (try
    (.setPrompt @jline-reader (get-prompt *ns*))
    (let [input-stream (clojure.lang.LineNumberingPushbackReader.
                         (JlineInputReader.
                           {:jline-reader @jline-reader
                            :set-empty-prompt set-empty-prompt})
                         1)]
        (do
          (Thread/interrupted) ; just to clear the status
          (actual-read input-stream request-prompt request-exit)))
    (catch InterruptedException e
      (clear-jline-buffer)
      request-prompt)
    (catch RuntimeException e
      (if (= InterruptedException (type (repl-exception e)))
        (do (clear-jline-buffer)
            request-prompt)
        (throw e)))))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)
  (reset! jline-reader (make-reader))

  (repl :read jline-read
        :prompt (fn [] false)
        :need-prompt (fn [] false))

  (.flush (.getHistory @jline-reader)))

