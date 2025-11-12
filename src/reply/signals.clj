(ns reply.signals)

(defmacro set-signal-handler! [signal f]
  (if (try (Class/forName "sun.misc.Signal")
          (catch Throwable e))
    `(try
       (sun.misc.Signal/handle
         (sun.misc.Signal. ~signal)
         (proxy [sun.misc.SignalHandler] []
           (handle [signal#] (~f signal#))))
       ; unrecognized signal - CONT on Windows, for instance
       (catch Throwable e#))
    `(println "Unable to set signal handlers.")))

