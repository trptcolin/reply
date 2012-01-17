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
                "")
        non-word-chars ["\\s" ","
                        "\\(" "\\)"
                        "\\[" "\\]"
                        "\\{" "\\}"]
        word-pattern-str (str "[^" (apply str non-word-chars) "]+")
        nonword-pattern-str (str "[" (apply str non-word-chars) "]+")
        groups (re-seq (re-pattern (str word-pattern-str "|"
                                        nonword-pattern-str))
                       start)]
     (if groups
       (if-let [last-word (last (re-seq #"[^\s\(\),]+" (last groups)))]
         last-word
         "")
        "")))

