(ns reply.completion
  (:require [clojure.string :as str]))

(defn get-last-word [input]
  (let [word (last (str/split input #"[^\w\-]"))]
    (if (empty? word)
      nil
      word)))

(defn but-last-word [input]
  (first (str/split input (re-pattern (str (get-last-word input) "$")))))

(defn get-candidates [candidates word-start]
  (filter (fn [w] (.startsWith w word-start))
          candidates))

