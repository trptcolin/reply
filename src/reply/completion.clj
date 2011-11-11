(ns reply.completion
  (:require [clojure.string :as str]))

(defn get-candidates [candidates word-start]
  (filter (fn [w] (.startsWith w word-start))
          candidates))

(defn get-unambiguous-completion [candidates]
  (if-let [candidates (seq candidates)]
    (apply str
      (map first
           (take-while #(apply = %)
                       (apply map vector candidates))))
    ""))

(defn get-word-ending-at [input index]
  (let [start (if (>= (.length input) index)
                (.substring input 0 index)
                "")]
    (or (last (str/split start #"[^\w\-]"))
        "")))

