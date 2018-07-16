(ns reply.reader.simple-jline
  (:require [reply.reader.jline.completion :as jline.completion])
  (:import [java.io File FileInputStream FileDescriptor
            PrintStream ByteArrayOutputStream IOException]
           [jline.console ConsoleReader ConsoleKeys]
           [jline.console.history FileHistory MemoryHistory]
           [jline.internal Configuration Log]))

(def ^:private current-console-reader (atom nil))

(defn- make-history-file [^String history-path]
  (if history-path
    (let [history-file (File. history-path)]
      (if (.getParentFile history-file)
        history-file
        (File. "." history-path)))
    (File. (System/getProperty "user.home") ".jline-reply.history")))

(defn reset-reader [^ConsoleReader reader]
  (when reader
    (.clear (.getCursorBuffer reader))))

(defn shutdown
  ([] (shutdown {:reader @current-console-reader}))
  ([{:keys [^ConsoleReader reader] :as state}]
   (when reader
     (reset-reader reader)
     (.restore (.getTerminal reader))
     (.shutdown reader))
   (reset! current-console-reader nil)))

(defn null-output-stream []
  (proxy [java.io.OutputStream] []
    (write [& args])))

(defn set-jline-output! []
  (when (and (not (Boolean/getBoolean "jline.internal.Log.trace"))
             (not (Boolean/getBoolean "jline.internal.Log.debug")))
    (Log/setOutput (PrintStream. ^java.io.OutputStream (null-output-stream)))))

(defn- initialize-jline []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(when-let [reader @current-console-reader]
                                (shutdown {:reader reader}))))
  (when (= "dumb" (System/getenv "TERM"))
    (.setProperty (Configuration/getProperties) "jline.terminal" "none"))
  (set-jline-output!))

(defmulti flush-history type)
(defmethod flush-history FileHistory
  [^FileHistory history]
  (try (.flush history)
    (catch IOException e)))
(defmethod flush-history MemoryHistory
  [history])

(defprotocol InteractiveLineReader
  (interactive-read-line [this])
  (prepare-for-next-read [this]))

(extend-protocol InteractiveLineReader
  ConsoleReader
  (interactive-read-line [reader]
    (try (.readLine reader)
         (catch UnsupportedOperationException e "")))
  (prepare-for-next-read [reader]
    (flush-history (.getHistory reader))
    (when-let [completer (first (.getCompleters reader))]
      (.removeCompleter reader completer))))

(defn ^Integer default-history-size
  "Get default history size from system settings, or nil if not set.
Set `app-name` for jline's reading of inputrc, or null for default."
  [^String app-name]
  (try
    (-> (ConsoleKeys. app-name (ConsoleReader/getInputRc))
        (.getVariable "history-size")
        (Integer/parseInt))
    (catch IOException ioe
      nil)))

(defn configure-history
  "Configure a MemoryHistory or FileHistory's max-size, if size
non-nil. Return nil."
  [hist ^Integer size]
  (when size
    (.setMaxSize hist size)))

(defn setup-console-reader
  [{:keys [prompt-string reader input-stream output-stream
           history-file completer-factory blink-parens]
    :or {input-stream (FileInputStream. FileDescriptor/in)
         output-stream System/out
         prompt-string "=> "
         blink-parens true}
    :as state}]
  (let [reader (ConsoleReader. input-stream output-stream)
        hist-size (default-history-size nil) ;; currently null appName
        file-history (doto (FileHistory. (make-history-file history-file) false)
                       (configure-history hist-size)
                       (.load))
        history (try
                  (flush-history file-history)
                  file-history
                  (catch IOException e
                    (doto (MemoryHistory.)
                      (configure-history hist-size))))
        completer (if completer-factory
                    (completer-factory reader)
                    nil)]
    (.setBlinkMatchingParen (.getKeys reader) blink-parens)
    (when completer (.addCompleter reader completer))
    (reset! current-console-reader reader)
    (doto reader
      (.setHistory history)
      (.setHandleUserInterrupt true)
      (.setExpandEvents false)
      (.setPaginationEnabled true)
      (.setPrompt prompt-string))))

(def jline-state (atom {}))

(defn get-input-line [state]
  (when-not (:reader state)
    (initialize-jline))
  (shutdown state)
  (if (:no-jline state)
    (assoc (dissoc state :no-jline)
           :reader nil
           :input (read-line))
    (let [reader (setup-console-reader state)
          input (try (interactive-read-line reader)
                  (catch jline.console.UserInterruptException e
                    :interrupted))]
      (prepare-for-next-read reader)
      (if (= :interrupted input)
        (assoc state
               :reader reader
               :input ""
               :interrupted true)
        (assoc state
               :reader reader
               :input input
               :interrupted nil)))))

(defn make-completer [ns eval-fn]
  (fn [^ConsoleReader reader]
    (let [redraw-line-fn (fn []
                           (.redrawLine reader)
                           (.flush reader))]
      (if ns
        (jline.completion/make-completer
          eval-fn redraw-line-fn ns)
        nil))))

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

