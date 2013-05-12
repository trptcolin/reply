(ns reply.completion
  (:require [clojure.string :as str]))

(def non-word-chars
  ["\\s" ","
   "\\(" "\\)"
   "\\[" "\\]"
   "\\{" "\\}"])

(defn input-up-to [input index]
  (let [topmost-index (min index (.length input))]
    (.substring input 0 topmost-index)))

(defn not-in-charset [characters]
  (str "[^" (apply str characters) "]+"))

(defn in-charset [characters]
  (str "[" (apply str characters) "]+"))

(def word-pattern-string (not-in-charset non-word-chars))
(def nonword-pattern-string (in-charset non-word-chars))

(defn last-word [input]
  (-> (str word-pattern-string "|" nonword-pattern-string)
      re-pattern
      (re-seq input)
      last))

(defn get-word-ending-at [input index]
  (let [input-start (input-up-to input index)
        last-word (or (last-word input-start) "")]
    (if (re-seq (re-pattern word-pattern-string) last-word)
      (str/replace last-word #"[':]" "")
      "")))

