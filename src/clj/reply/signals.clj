(ns reply.signals)

(defn set-signal-handler! [signal f]
  (try
    (sun.misc.Signal/handle
      (sun.misc.Signal. signal)
      (proxy [sun.misc.SignalHandler] []
        (handle [signal] (f signal))))
    (catch IllegalArgumentException e
      ; unrecognized signal - CONT on Windows, for instance
      )))

