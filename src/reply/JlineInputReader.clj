(ns reply.JlineInputReader
  (:gen-class
   :extends java.io.Reader
   :state state
   :init init
   :constructors {[clojure.lang.Associative] []}
   :main false))

(defn -init [config]
  [[] (atom (assoc config
                   :internal-queue (java.util.LinkedList.)))])

(defn -read-single [this]
  (let [state @(.state this)
        input-queue (:internal-queue state)
        jline-reader (:jline-reader state)
        set-empty-prompt (:set-empty-prompt state)]

    (if-let [c (.peekFirst input-queue)]
      (.removeFirst input-queue)
      (let [line (.readLine jline-reader)]
        (set-empty-prompt)
        (if line
          (do
            (doseq [c line]
              (.addLast input-queue (int c)))
            (.addLast input-queue (int \newline))
            (-read-single this))
          -1)))))

(defn -read-char<>-int-int [this cbuf off len]
  (let [state @(.state this)
        input-queue (:internal-queue state)
        jline-reader (:jline-reader state)]
    (loop [i   off
           left len]
      (if (> left 0)
        (let [c (-read-single this)]
          (if (= c -1)
            (if (= i off)
              -1
              (- i off))
            (do (aset-char cbuf i c)
                (recur (inc i) (dec left)))))
        (- i off)))))

