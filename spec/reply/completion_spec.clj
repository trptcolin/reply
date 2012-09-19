(ns reply.completion-spec
  (:use [reply.completion]
        [speclj.core]))

(describe "get-word-ending-at"
  (it "finds no words when there's nothing there"
    (should= "" (get-word-ending-at "" 0))
    (should= "" (get-word-ending-at " " 0))
    (should= "" (get-word-ending-at "map" 0)))

  (it "finds a plain symbol"
    (should= "ma" (get-word-ending-at "map" 2))
    (should= "map" (get-word-ending-at "map" 3))
    (should= "map" (get-word-ending-at "(map first [0 1 2])" 4)))

  (it "finds a namespace"
    (should= "clojure.c" (get-word-ending-at "clojure.c" 9))
    (should= "clojure.c" (get-word-ending-at "'clojure.c" 10)))

  (it "omits brackets of several sorts (but takes asterisks)"
    (should= "" (get-word-ending-at "(map " 5))
    (should= "*foo" (get-word-ending-at "[*foo" 5))
    (should= "*foo" (get-word-ending-at "{*foo" 5)))

  (it "includes slashes (for namespaces and classes)"
    (should= "str/split" (get-word-ending-at "str/split" 9))
    (should= "Integer/MAX_VALUE" (get-word-ending-at "Integer/MAX_VALUE" 17))))
