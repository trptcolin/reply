(ns reply.reader.jline
  (:refer-clojure :exclude [read])
  (:require [reply.reader.jline.completion :as jline.completion]
            [reply.eval-state :as eval-state]
            [reply.reader.simple-jline :as simple-jline]
            [clojure.main])
  (:import [java.io File IOException PrintStream ByteArrayOutputStream
            FileInputStream FileDescriptor]
           [reply.reader.jline JlineInputReader]
           [reply.hacks CustomizableBufferLineNumberingPushbackReader]
           [jline.console ConsoleReader]
           [jline.console.history FileHistory]
           [jline.internal Configuration Log]))

(def jline-reader (atom nil))
(def jline-pushback-reader (atom nil))

(def prompt-end "=> ")

(defmulti get-prompt type)
(defmethod get-prompt :default [ns]
  (format (str "%s" prompt-end) ns))
(defmethod get-prompt clojure.lang.Namespace [ns]
  (get-prompt (ns-name ns)))

(def prompt-fn (atom get-prompt))
(defn set-prompt-fn! [f]
  (when f (reset! prompt-fn f)))

(defn set-empty-prompt []
  (let [prompt-end (str "#_" prompt-end)]
    (.setPrompt
      @jline-reader
      (apply str
        (concat (repeat (- (count (@prompt-fn (eval-state/get-ns)))
                           (count prompt-end))
                        \space)
                prompt-end)))))

(defn setup-reader! [options]
  (when-not (System/getenv "JLINE_LOGGING")
    (Log/setOutput (PrintStream. (ByteArrayOutputStream.))))
  ; since construction is side-effect-y
  (reset! jline-reader (simple-jline/setup-console-reader options))
  ; since this depends on jline-reader
  (reset! jline-pushback-reader
    (CustomizableBufferLineNumberingPushbackReader.
      (JlineInputReader.
        {:jline-reader @jline-reader
         :set-empty-prompt set-empty-prompt})
      1)))

(defn prepare-for-read [eval-fn ns]
  (simple-jline/prepare-for-next-read {:reader @jline-reader})
  (.setPrompt @jline-reader (@prompt-fn ns))
  (eval-state/set-ns ns)
  (.addCompleter @jline-reader
    ((simple-jline/make-completer (str (ns-name ns)) eval-fn)
       @jline-reader)))

(defmacro with-jline-in [& body]
  `(do
    (try
      (setup-reader! {})
      (prepare-for-read reply.initialization/eval-in-user-ns
                        (eval-state/get-ns))
      (binding [*in* @jline-pushback-reader]
        (Thread/interrupted) ; just to clear the status
        ~@body)
      ; NOTE: this indirection is for wrapped exceptions in 1.3
      (catch Throwable e#
        (if (#{IOException InterruptedException}
               (type (clojure.main/repl-exception e#)))
          (do (simple-jline/reset-reader @jline-reader) nil)
          (throw e#))))))

(defn read [request-prompt request-exit]
  (with-jline-in
    (clojure.main/repl-read request-prompt request-exit)))

