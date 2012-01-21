(ns reply.completion-test
  (:use [reply.completion]
        [midje.sweet]))

(facts "get-word-ending-at"
  (get-word-ending-at "" 0) => ""
  (get-word-ending-at " " 0) => ""
  (get-word-ending-at "map" 0) => ""
  (get-word-ending-at "map" 2) => "ma"
  (get-word-ending-at "map" 3) => "map"
  (get-word-ending-at "(map first [0 1 2])" 4) => "map"
  (get-word-ending-at "clojure.c" 9) => "clojure.c"
  (get-word-ending-at "(map " 5) => ""
  (get-word-ending-at "[*foo" 5) => "*foo"
  (get-word-ending-at "{*foo" 5) => "*foo"
  (get-word-ending-at "str/split" 9) => "str/split")

