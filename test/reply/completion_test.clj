(ns reply.completion-test
  (:use [reply.completion]
        [midje.sweet]))

(facts "get-last-word"
  (get-last-word "") => nil
  (get-last-word " ") => nil
  (get-last-word "ohai") => "ohai"
  (get-last-word "ohai kthx") => "kthx"
  (get-last-word "(map") => "map"
  (get-last-word "(map-indexed") => "map-indexed")

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


