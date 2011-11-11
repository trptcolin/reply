(ns reply.completion-test
  (:use [reply.completion]
        [midje.sweet]))

(facts "get-candidates"
  (get-candidates [] "map") => []
  (get-candidates ["map"] "foo") => []
  (get-candidates ["map-indexed"] "map-indexed") => ["map-indexed"]
  (get-candidates ["map" "map-indexed"] "map") => ["map" "map-indexed"]
  (get-candidates ["map" "map-indexed"] "map-in") => ["map-indexed"])

(facts "get-unambiguous-completion"
  (get-unambiguous-completion []) => ""
  (get-unambiguous-completion ["map"]) => "map"
  (get-unambiguous-completion ["map", "map-indexed"]) => "map")

(facts "get-word-ending-at"
  (get-word-ending-at "" 0) => ""
  (get-word-ending-at " " 0) => ""
  (get-word-ending-at "map" 0) => ""
  (get-word-ending-at "map" 2) => "ma"
  (get-word-ending-at "map" 3) => "map"
  (get-word-ending-at "(map first [0 1 2])" 4) => "map"
  )

