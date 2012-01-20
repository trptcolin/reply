(ns reply.reader.jline
  (:refer-clojure :exclude [read])
  (:use [clojure.main :only [repl-read repl-exception]])
  (:require [reply.reader.jline.completion :as jline.completion]
            [reply.eval-state :as eval-state])
  (:import [java.io File IOException PrintStream ByteArrayOutputStream]
           [reply.reader.jline JlineInputReader]
           [reply.hacks CustomizableBufferLineNumberingPushbackReader]
           [jline.console ConsoleReader]
           [jline.console.history FileHistory]
           [jline.internal Log]))

(def jline-reader (atom nil))
(def jline-pushback-reader (atom nil))

(defn make-reader []
  (let [reader (ConsoleReader.)
        home (System/getProperty "user.home")
        history (FileHistory. (File. home ".jline-reply.history"))
        completer (jline.completion/make-var-completer 'clojure.core)
        completion-handler (jline.completion/make-completion-handler)]

    (doto reader
      (.setHistory history)
      (.setPaginationEnabled true)
      (.setCompletionHandler completion-handler)
      (.addCompleter completer))))

(defn get-prompt [ns]
  (format "%s=> " (ns-name ns)))

(defn set-empty-prompt []
  (.setPrompt
    @jline-reader
    (apply str (repeat (count (get-prompt (eval-state/get-ns))) \space))))

(defn setup-reader! []
  (Log/setOutput (PrintStream. (ByteArrayOutputStream.)))
  (reset! jline-reader (make-reader)) ; since construction is side-effect-y
  (reset! jline-pushback-reader ; since this depends on jline-reader
    (CustomizableBufferLineNumberingPushbackReader.
      (JlineInputReader.
        {:jline-reader @jline-reader
         :set-empty-prompt set-empty-prompt})
      1)))

(defn reset-reader []
  (-> (.getCursorBuffer @jline-reader)
      (.clear)))

(defn resume-reader []
  (.init (.getTerminal @jline-reader))
  (.redrawLine @jline-reader)
  (.flush @jline-reader))

(defn shutdown-reader []
  (.restore (.getTerminal @jline-reader)))

(defn actual-read [input-reader request-prompt request-exit]
  (binding [*in* input-reader]
    (repl-read request-prompt request-exit)))

(defn read [request-prompt request-exit]
  (when-not @jline-reader (setup-reader!))
  (try
    (.flush (.getHistory @jline-reader))
    (.setPrompt @jline-reader (get-prompt (eval-state/get-ns)))
    (let [completer (first (.getCompleters @jline-reader))]
      (.removeCompleter @jline-reader completer)
      (.addCompleter @jline-reader (jline.completion/make-var-completer (eval-state/get-ns))))
    (let [input-stream @jline-pushback-reader]
      (do
        (Thread/interrupted) ; just to clear the status
        (actual-read input-stream request-prompt request-exit)))
    ; NOTE: this indirection is for wrapped exceptions in 1.3
    (catch Throwable e
      (if (#{IOException InterruptedException} (type (repl-exception e)))
        (do
          (reset-reader)
          request-prompt)
        (throw e)))))
