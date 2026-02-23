(ns reply.reader.simple-jline
  (:require [reply.reader.jline.completion :as jline.completion])
  (:import [java.io BufferedReader File FileInputStream FileDescriptor
            InputStreamReader IOException OutputStream]
           [java.util.logging Logger Level]
           [org.jline.reader LineReader LineReader$Option LineReaderBuilder
            UserInterruptException EndOfFileException]
           [org.jline.reader.impl.history DefaultHistory]
           [org.jline.terminal Terminal TerminalBuilder]))

(def ^:private current-console-reader (atom nil))
(def ^:private current-terminal (atom nil))
(def ^:private current-piped-reader (atom nil))
(def ^:private current-piped-stream (atom nil))

(def ^:private default-history-size-value 500)

(defn- make-history-file [^String history-path]
  (if history-path
    (let [history-file (File. history-path)]
      (if (.getParentFile history-file)
        history-file
        (File. "." history-path)))
    (File. (System/getProperty "user.home") ".jline-reply.history")))

(defn shutdown
  ([] (shutdown {:reader @current-console-reader}))
  ([{:keys [^LineReader reader] :as state}]
   (when-let [^Terminal terminal @current-terminal]
     (.close terminal))
   (reset! current-console-reader nil)
   (reset! current-terminal nil)))

(defn- set-jline-log-level! []
  (let [logger (Logger/getLogger "org.jline")]
    (.setLevel logger Level/SEVERE)))

(defn- migrate-history-file
  "Rename old jline2-format history file so jline3 can start fresh."
  [^File history-file]
  (when (.exists history-file)
    (try
      (let [first-line (with-open [rdr (clojure.java.io/reader history-file)]
                         (.readLine rdr))]
        (when (and first-line
                   (not (re-find #"^\d+:" first-line)))
          (.renameTo history-file (File. (str (.getPath history-file) ".old")))))
      (catch Exception _))))

(defn- initialize-jline []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(when-let [reader @current-console-reader]
                                (shutdown {:reader reader}))))
  (set-jline-log-level!))

(defn flush-history [^DefaultHistory history]
  (try (.save history)
    (catch IOException e)))

(defprotocol InteractiveLineReader
  (interactive-read-line [this])
  (prepare-for-next-read [this]))

(extend-protocol InteractiveLineReader
  LineReader
  (interactive-read-line [reader]
    (try (.readLine reader)
         (catch UnsupportedOperationException e "")))
  (prepare-for-next-read [reader]
    (flush-history (.getHistory reader))))

(defn setup-console-reader
  [{:keys [prompt-string reader input-stream output-stream
           history-file completer-factory blink-parens]
    :or {prompt-string "=> "
         blink-parens true}
    :as state}]
  (let [tb (TerminalBuilder/builder)
        _ (when (= "dumb" (System/getenv "TERM"))
            (.dumb tb true))
        terminal (.build tb)
        hist-file (make-history-file history-file)
        _ (migrate-history-file hist-file)
        history-path (.getAbsolutePath hist-file)
        completer (when completer-factory
                    (completer-factory nil))
        builder (-> (LineReaderBuilder/builder)
                    (.terminal terminal)
                    (.variable LineReader/HISTORY_FILE (java.nio.file.Paths/get
                                                        history-path
                                                        (into-array String [])))
                    (.variable LineReader/HISTORY_SIZE (int default-history-size-value)))
        builder (if completer
                  (.completer builder completer)
                  builder)
        ^LineReader reader (.build builder)]
    (.setOpt reader LineReader$Option/DISABLE_EVENT_EXPANSION)
    (reset! current-console-reader reader)
    (reset! current-terminal terminal)
    reader))

(def jline-state (atom {}))

(defn- piped-read-line
  "Read a line from a custom input stream, printing the prompt to output-stream.
  Used when input is piped (non-interactive), since jline3's DumbTerminal
  does not support reading from ByteArrayInputStream."
  [{:keys [input-stream output-stream prompt-string]
    :or {prompt-string "=> "}}]
  (let [^BufferedReader br (if (and @current-piped-reader
                                    (identical? @current-piped-stream input-stream))
                               @current-piped-reader
                               (let [r (BufferedReader. (InputStreamReader. input-stream))]
                                 (reset! current-piped-reader r)
                                 (reset! current-piped-stream input-stream)
                                 r))
        ^OutputStream os (or output-stream System/out)]
    (.write os (.getBytes ^String prompt-string))
    (.flush os)
    (.readLine br)))

(defn get-input-line [state]
  (when-not (:reader state)
    (initialize-jline))
  (cond
    (:no-jline state)
    (assoc (dissoc state :no-jline)
           :reader nil
           :input (read-line))

    (:input-stream state)
    (let [input (piped-read-line state)]
      (assoc state
             :reader nil
             :input input
             :interrupted nil))

    :else
    (do
      (shutdown state)
      (let [reader (setup-console-reader state)
            prompt (or (:prompt-string state) "=> ")
            input (try (.readLine reader prompt)
                    (catch UserInterruptException e
                      :interrupted)
                    (catch EndOfFileException e
                      nil))]
        (prepare-for-next-read reader)
        (if (= :interrupted input)
          (assoc state
                 :reader reader
                 :input ""
                 :interrupted true)
          (assoc state
                 :reader reader
                 :input input
                 :interrupted nil))))))

(defn make-completer [ns eval-fn]
  (fn [_reader]
    (if ns
      (jline.completion/make-completer eval-fn ns)
      nil)))

(defn safe-read-line
  ([{:keys [prompt-string completer-factory no-jline input-stream output-stream
            history-file]
     :as options}]
   (swap! jline-state
          assoc
          :no-jline no-jline
          :history-file history-file
          :prompt-string prompt-string
          :completer-factory completer-factory)
   (when input-stream
     (swap! jline-state assoc :input-stream input-stream))
   (when output-stream
     (swap! jline-state assoc :output-stream output-stream))
   (swap! jline-state get-input-line)
   (if (:interrupted @jline-state) ;; TODO: don't do this same check in 2 places
     :interrupted
     (:input @jline-state)))
  ([completion-eval-fn
    {:keys [ns] :as state}]
   (safe-read-line
     (assoc state :completer-factory (make-completer ns completion-eval-fn)))))
