(ns reply.core
  (:use [clojure.main :only [repl repl-read repl-exception]]
        [clojure.repl :only [set-break-handler!]])
  (:require [reply.hacks.printing]
            [reply.completion.jline :as completion.jline]
            [clojure.string :as str])
  (:import [reply JlineInputReader]
           [reply.hacks CustomizableBufferLineNumberingPushbackReader]
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
        history (FileHistory. (File. home ".jline-reply.history"))
        completer (completion.jline/make-completer
                    (map str (keys (ns-publics 'clojure.core))))
        completion-handler (completion.jline/make-completion-handler)]
    (doto reader
      (.setBellEnabled false)
      (.setHistory history)
      (.setCompletionHandler completion-handler)
      (.addCompleter completer))))

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

(def jline-pushback-reader (atom nil))

(defn jline-read [request-prompt request-exit]
  (try
    (.setPrompt @jline-reader (get-prompt *ns*))
    (let [input-stream @jline-pushback-reader]
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
  (reset! jline-pushback-reader
    (CustomizableBufferLineNumberingPushbackReader.
      (JlineInputReader.
        {:jline-reader @jline-reader
         :set-empty-prompt set-empty-prompt})
      1))
  (repl :read jline-read
        :prompt (fn [] false)
        :need-prompt (fn [] false))

  (.flush (.getHistory @jline-reader)))

