(ns reply.reader.jline.JlineInputReader
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
  (let [{:keys [^java.util.Deque internal-queue jline-reader set-empty-prompt]}
        @(.state this)]
    (if-let [c (.peekFirst internal-queue)]
      (.removeFirst internal-queue)
      (let [line (.readLine jline-reader)]
        (set-empty-prompt)
        (if line
          (do
            (doseq [c line]
              (.addLast internal-queue (int c)))
            (.addLast internal-queue (int \newline))
            (-read-single this))
          -1)))))

(defn -read-char<>-int-int [this buffer offset length]
  (let [{:keys [internal-queue jline-reader]}
        @(.state this)]
    (loop [i    offset
           left length]
      (if (> left 0)
        (let [c (-read-single this)]
          (if (= c -1)
            (if (= i offset)
              -1
              (- i offset))
            (do (aset-char buffer i c)
                (recur (inc i) (dec left)))))
        (- i offset)))))

