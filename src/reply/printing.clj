(ns reply.printing)

(in-ns 'clojure.core)
(defn- print-sequential [^String begin, print-one, ^String sep, ^String end, sequence, ^Writer w]
  (binding [*print-level* (and (not *print-dup*) *print-level* (dec *print-level*))]
    (if (and *print-level* (neg? *print-level*))
      (.write w "#")
      (do
        (.write w begin)
        (when-let [xs (seq sequence)]
          (if (and (not *print-dup*) *print-length*)
            (loop [[x & xs] xs
                   print-length *print-length*]
              (when (Thread/interrupted)
                (.flush w)
                (throw (java.io.InterruptedIOException.)))
              (if (zero? print-length)
                (.write w "...")
                (do
                  (print-one x w)
                  (when xs
                    (.write w sep)
                    (recur xs (dec print-length))))))
            (loop [[x & xs] xs]
              (when (Thread/interrupted)
                (.flush w)
                (throw (java.io.InterruptedIOException.)))
              (print-one x w)
              (when xs
                (.write w sep)
                (recur xs)))))
        (.write w end)))))
