(ns reply.hacks.printing)

(defn print-sequential
  "The monkey-patch here is to be a good citizen in the face of thread
  interruption. Only the (when (Thread/interrupted) ...) forms are
  different from the clojure.core implementation."
  [^String begin, print-one, ^String sep, ^String end, sequence, ^java.io.Writer w]
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

(in-ns 'clojure.core)
(defmethod print-method java.lang.Double [^Double d, ^Writer w]
  (let [result-str
          (cond
            (= d Double/POSITIVE_INFINITY) "Double/POSITIVE_INFINITY"
            (= d Double/NEGATIVE_INFINITY) "Double/NEGATIVE_INFINITY"
            (.isNaN d) "Double/NaN"
            :else (str d))]
    (.write w result-str)))

(in-ns 'reply.hacks.printing)
