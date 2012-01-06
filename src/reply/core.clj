(ns reply.core
  (:use [clojure.main :only [repl repl-read repl-exception]]
        [clojure.repl :only [set-break-handler!]])
  (:require [reply.hacks.printing :as hacks.printing]
            [reply.completion.jline :as completion.jline]
            [clojure.string :as str])
  (:import [reply JlineInputReader]
           [reply.hacks CustomizableBufferLineNumberingPushbackReader]
           [scala.tools.jline.console ConsoleReader]
           [scala.tools.jline.console.history FileHistory]
           [scala.tools.jline.internal Log]
           [java.io File IOException]))

(defn actual-read [input-reader request-prompt request-exit]
  (binding [*in* input-reader]
    (repl-read request-prompt request-exit)))

(def main-thread (Thread/currentThread))

(defn make-reader []
  (let [reader (ConsoleReader.)
        home (System/getProperty "user.home")
        history (FileHistory. (File. home ".jline-reply.history"))
        completer (completion.jline/make-var-completer 'clojure.core)
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

(def evaling-line (atom nil))

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)

  ; for printing
  (.interrupt main-thread)

  ; for eval
  (let [line @evaling-line
        thread (:thread line)]
    (when thread
      (.interrupt thread)

      ; wait a bit, then STOP HARD
      (Thread/sleep 1000)
      (if (and (not (:completed line)) (.isAlive thread))
        (.stop thread))
  ))

  (clear-jline-buffer))

(defn set-empty-prompt []
  (.setPrompt
    @jline-reader
    (apply str
           (concat (repeat (- (count (get-prompt *ns*)) 2) \space)
                   [\| \space]))))

(def jline-pushback-reader (atom nil))

(defn jline-read [request-prompt request-exit]
  (reset! evaling-line nil)
  (try
    (.setPrompt @jline-reader (get-prompt *ns*))
    (let [completer (first (.getCompleters @jline-reader))]
      (.removeCompleter @jline-reader completer)
      (.addCompleter @jline-reader (completion.jline/make-var-completer *ns*)))
    (let [input-stream @jline-pushback-reader]
      (do
        (Thread/interrupted) ; just to clear the status
        (actual-read input-stream request-prompt request-exit)))
    (catch InterruptedException e
      (clear-jline-buffer)
      request-prompt)
    (catch IOException e
      (clear-jline-buffer)
      request-prompt)
    ; NOTE: this weirdness is for wrapped exceptions in 1.3
    (catch Throwable e
      (if (#{IOException InterruptedException} (type (repl-exception e)))
        (do
          (clear-jline-buffer)
          request-prompt)
        (throw e)))))

(defn setup-reader! []
  (Log/setOutput (java.io.PrintStream. (java.io.ByteArrayOutputStream.)))

  (reset! jline-reader (make-reader)) ; since construction is side-effect-y
  (reset! jline-pushback-reader ; since this depends on jline-reader
    (CustomizableBufferLineNumberingPushbackReader.
      (JlineInputReader.
        {:jline-reader @jline-reader
         :set-empty-prompt set-empty-prompt})
      1)))

(defn reply-eval [form]
  @(future
    (reset! evaling-line {:thread (Thread/currentThread)})
    (let [result (eval form)]
      (swap! evaling-line assoc :completed true)
      result)))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)
  (setup-reader!)
  (println "Clojure" (clojure-version))

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential]
    (repl :read jline-read
          :eval reply-eval
          :prompt (fn [] false)
          :need-prompt (fn [] false)))

  (shutdown-agents)

  (.flush (.getHistory @jline-reader)))

