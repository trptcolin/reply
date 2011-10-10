(ns reply.core
  (:use [clojure.main :only [repl repl-read]]
        [clojure.repl :only [set-break-handler!]])
  (:require [reply.printing])
  (:import [scala.tools.jline.console ConsoleReader]
           [scala.tools.jline.console.history FileHistory]
           [java.io File]))

(defn actual-read [input request-prompt request-exit]
  (binding [*in* (clojure.lang.LineNumberingPushbackReader. input)]
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

(def current-repl-ns (atom *ns*))
(defn get-prompt [ns]
  (format "%s=> " (ns-name ns)))

(def output-pipe (java.io.PipedWriter.))
(def input-pipe (java.io.PipedReader. output-pipe))

(defn clear-jline-buffer []
  (-> (.getCursorBuffer jline-reader)
      (.clear)))

(defn handle-ctrl-c [signal]
  (println "^C")
  (.interrupt main-thread)
  (clear-jline-buffer)
  (flush))

(defn jline-read [request-prompt request-exit]
  (try
    (.setPrompt jline-reader (get-prompt *ns*))
    (reset! current-repl-ns *ns*)
    (let [future-input (future
                         ; TODO: need to loop here to read multiple lines
                         (try
                            (let [string-input (.readLine jline-reader)]
                              (if string-input
                                (do
                                  (doseq [c (map int (.getBytes string-input))]
                                    (.write output-pipe c)
                                    (.flush output-pipe))
                                  (.write output-pipe (int \newline)))
                                (.close output-pipe)))
                         (catch Throwable e
                           (prn e)
                           (throw e))))
          _ @future-input
          input input-pipe]
        (do
          (Thread/interrupted) ; just to clear the status
          (actual-read input-pipe request-prompt request-exit)))
    (catch InterruptedException e
      (clear-jline-buffer)
      request-prompt)
    (catch Throwable e
      (prn e))))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)

  (repl :read jline-read
        :prompt (constantly ""))

  (shutdown-agents)

  (.flush (.getHistory jline-reader)))

