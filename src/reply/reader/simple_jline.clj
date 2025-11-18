(ns reply.reader.simple-jline
  (:require [reply.reader.jline.completion :as completion])
  (:import (java.io FileInputStream FileDescriptor)
           (java.nio.file Paths)
           (org.jline.reader LineReader LineReaderBuilder)
           (org.jline.terminal TerminalBuilder)
           (org.jline.reader.impl.history DefaultHistory)))

(defn setup-reader [{:keys [input-stream output-stream
                            history-file completer-factory]
                     :or {input-stream (FileInputStream. FileDescriptor/in)
                          output-stream System/out}
                     :as _state}]
  (let [terminal (-> (TerminalBuilder/builder)
                     (.streams input-stream output-stream)
                     .build)
        history-path (if history-file
                       (Paths/get history-file)
                       (Paths/get (System/getProperty "user.home")
                                  (into-array [".jline3-reply.history"])))
        reader (-> (LineReaderBuilder/builder)
                   (.terminal terminal)
                   (.history (DefaultHistory.))
                   (.variable LineReader/HISTORY_FILE history-path)
                   (.build))]
    ;; (when completer-factory
    ;;   (.addCompleter reader (completer-factory reader)))
    reader))

(def ^:private jline-state (atom {}))

(defn- get-input-line [state reader]
  (if (:no-jline state)
    (assoc (dissoc state :no-jline)
           :reader nil
           :input (read-line))
    (let [input (try (.readLine reader (:prompt-string state))
                     (catch org.jline.reader.UserInterruptException _
                       :interrupted))]
      ;; (when-let [completer (first (.getCompleters reader))]
      ;;   (.removeCompleter reader completer))
      (if (= :interrupted input)
        (assoc state
               :reader reader
               :input ""
               :interrupted true)
        (assoc state
               :reader reader
               :input input
               :interrupted nil)))))

(defn- make-completer [ns eval-fn]
  (fn [^LineReader reader]
    (let [redraw-line-fn (fn []
                           (.redrawLine reader)
                           (.flush reader))]
      (if ns
        (completion/make-completer eval-fn redraw-line-fn ns)
        nil))))

(defn safe-read-line [{:keys [prompt-string completer-factory no-jline
                              input-stream output-stream reader
                              history-file ns completion-eval-fn]
    :as _options}]
  (swap! jline-state
         assoc
         :completer-factory (make-completer ns completion-eval-fn)
         :no-jline no-jline
         :history-file history-file
         :prompt-string prompt-string
         :completer-factory completer-factory)
  (when input-stream
    (swap! jline-state assoc :input-stream input-stream))
  (when output-stream
    (swap! jline-state assoc :output-stream output-stream))
  (swap! jline-state get-input-line reader)
  (if (:interrupted @jline-state) ;; TODO: don't do this same check in 2 places
    :interrupted
    (:input @jline-state)))
