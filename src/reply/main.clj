(ns reply.main
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
(def printing-line (atom nil))

(defn stop [action & {:keys [hard-kill-allowed]}]
  (let [thread (:thread @action)]
    (when thread (.interrupt thread))
    (when hard-kill-allowed
      (Thread/sleep 2000)
      (when (and @action (not (:completed @action)) (.isAlive thread))
        (println ";;;;;;;;;;")
        (println "; Sorry, have to call Thread.stop on this command, because it's not dying.")
        (println ";;;;;;;;;;")
        (.stop thread)))))

(defn set-empty-prompt []
  (.setPrompt
    @jline-reader
    (apply str
           (concat (repeat (- (count (get-prompt *ns*)) 2) \space)
                   [\| \space]))))

(def jline-pushback-reader (atom nil))

(defn jline-read [request-prompt request-exit]
  (reset! evaling-line nil)
  (reset! printing-line nil)
  (try
    (.setPrompt @jline-reader (get-prompt *ns*))
    (let [completer (first (.getCompleters @jline-reader))]
      (.removeCompleter @jline-reader completer)
      (.addCompleter @jline-reader (completion.jline/make-var-completer *ns*)))
    (let [input-stream @jline-pushback-reader]
      (do
        (Thread/interrupted) ; just to clear the status
        (actual-read input-stream request-prompt request-exit)))
    ; NOTE: this indirection is for wrapped exceptions in 1.3
    (catch Throwable e
      (if (#{IOException InterruptedException} (type (repl-exception e)))
        (do
          (clear-jline-buffer)
          request-prompt)
        (throw e)))))

(defn act-in-future [form action-atom base-action]
  (try
    (reset! action-atom {})
    (let [act-on-form (fn []
                        (swap! action-atom assoc :thread (Thread/currentThread))
                        (let [result (base-action form)]
                          (swap! action-atom assoc :completed true)
                          result))]
      @(future (act-on-form)))
    (catch Throwable e
      (println (repl-exception e))
      (.printStackTrace e))))

(defn reply-eval [form]
  (act-in-future form evaling-line eval))

(defn reply-print [form]
  (act-in-future form printing-line prn))

(defn handle-ctrl-c [signal]
  (print "^C")
  (flush)

  (stop printing-line)
  (stop evaling-line :hard-kill-allowed true)

  (clear-jline-buffer))

(defn setup-reader! []
  (Log/setOutput (java.io.PrintStream. (java.io.ByteArrayOutputStream.)))
  (reset! jline-reader (make-reader)) ; since construction is side-effect-y
  (reset! jline-pushback-reader ; since this depends on jline-reader
    (CustomizableBufferLineNumberingPushbackReader.
      (JlineInputReader.
        {:jline-reader @jline-reader
         :set-empty-prompt set-empty-prompt})
      1)))

(defn -main [& args]
  (set-break-handler! handle-ctrl-c)
  (setup-reader!)
  (println "Clojure" (clojure-version))

  (with-redefs [clojure.core/print-sequential hacks.printing/print-sequential]
    (repl :read jline-read
          :eval reply-eval
          :print reply-print
          :prompt (fn [] false)
          :need-prompt (fn [] false)))

  (shutdown-agents)

  (.flush (.getHistory @jline-reader)))

