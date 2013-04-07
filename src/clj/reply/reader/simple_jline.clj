(ns reply.reader.simple-jline
  (:require [reply.reader.jline.completion :as jline.completion])
  (:import [java.io File FileInputStream FileDescriptor
            PrintStream ByteArrayOutputStream]
           [jline.console ConsoleReader]
           [jline.console.history FileHistory]
           [jline.internal Configuration Log]))

(def ^:private current-console-reader (atom nil))

(defn- make-history-file [history-path]
  (if history-path
    (let [history-file (File. history-path)]
      (if (.getParentFile history-file)
        history-file
        (File. "." history-path)))
    (File. (System/getProperty "user.home") ".jline-reply.history")))

(defn shutdown [{:keys [reader] :as state}]
  (when reader
    (.clear (.getCursorBuffer reader))
    (.restore (.getTerminal reader))
    (.shutdown reader)))

(defn null-output-stream []
  (proxy [java.io.OutputStream] []
    (write [b])))

(defn- initialize-jline []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(when-let [reader @current-console-reader]
                                (shutdown reader))))
  (when (= "dumb" (System/getenv "TERM"))
    (.setProperty (Configuration/getProperties) "jline.terminal" "none"))
  (when-not (System/getenv "JLINE_LOGGING")
    (Log/setOutput (PrintStream. (null-output-stream)))))

(defn- prepare-for-next-read [{:keys [reader] :as state}]
  (.flush (.getHistory reader))
  (.removeCompleter reader (first (.getCompleters reader))))

(defn- setup-console-reader
  [{:keys [prompt-string reader input-stream output-stream
           history-file completer-factory blink-parens]
    :or {input-stream (FileInputStream. FileDescriptor/in)
         output-stream System/out
         prompt-string "=> "
         blink-parens true}
    :as state}]
  (let [reader (ConsoleReader. input-stream output-stream)
        history (FileHistory. (make-history-file history-file))
        ;; TODO: rip out this default completer, make it a no-op
        completer (if completer-factory
                    (completer-factory reader)
                    nil)]
    (.setBlinkMatchingParen (.getKeys reader) blink-parens)
    (when completer (.addCompleter reader completer))
    (doto reader
      (.setHistory history)
      (.setHandleUserInterrupt true)
      (.setExpandEvents false)
      (.setPaginationEnabled true)
      (.setPrompt prompt-string))))

(def jline-state (atom {}))

(defn get-input-line [state]
  (if (:reader state)
    (prepare-for-next-read state)
    (initialize-jline))
  (if (:no-jline state)
    (do
      (shutdown state)
      (assoc (dissoc state :no-jline)
           :reader nil
           :input (read-line)))
    (let [reader (setup-console-reader state)
          input (try (.readLine reader)
                  (catch jline.console.UserInterruptException e
                    :interrupted))]
      (if (= :interrupted input)
        (assoc state
               :reader reader
               :input ""
               :interrupted true)
        (assoc state
               :reader reader
               :input input
               :interrupted nil)))))

(defn safe-read-line
  [{:keys [prompt-string completer-factory no-jline input-stream] :as options}]
  (swap! jline-state
         assoc
         :no-jline no-jline
         :prompt-string prompt-string
         :completer-factory completer-factory)

  (when input-stream ; default args are janky
    (swap! jline-state assoc :input-stream input-stream))

  (swap! jline-state
         (fn [previous-state]
           (get-input-line previous-state)))
  (if (:interrupted @jline-state) ;; TODO: don't do this same check in 2 places
    :interrupted
    (:input @jline-state)))

