(ns reply.completion-spec
  (:use [reply.completion]
        [speclj.core]))

(describe "get-word-ending-at"
  (it "falls back to the last location if the index is too high"
    (should= "map" (get-word-ending-at "map" 20)))

  (it "finds no words when there's nothing there"
    (should= "" (get-word-ending-at "" 0))
    (should= "" (get-word-ending-at " " 0))
    (should= "" (get-word-ending-at "map" 0)))

  (it "finds a plain symbol"
    (should= "ma" (get-word-ending-at "map" 2))
    (should= "map" (get-word-ending-at "map" 3))
    (should= "map" (get-word-ending-at "(map first [0 1 2])" 4)))

  (it "finds namespaces, also as symbols or keywords"
    (should= "clojure.c" (get-word-ending-at "clojure.c" 9))
    (should= "clojure.c" (get-word-ending-at "'clojure.c" 10))
    (should= "clojure.c" (get-word-ending-at ":clojure.c" 10)))

  (it "omits brackets of several sorts (but takes asterisks)"
    (should= "*foo" (get-word-ending-at "(*foo" 5))
    (should= "*foo" (get-word-ending-at "[*foo" 5))
    (should= "*foo" (get-word-ending-at "{*foo" 5))
    (should= "foo" (get-word-ending-at "]foo" 4))
    (should= "foo" (get-word-ending-at "}foo" 4))
    (should= "foo" (get-word-ending-at ")foo" 4)))

  (it "gets nothing when the last segment is a non-word"
    (should= "" (get-word-ending-at "map " 4))
    (should= "" (get-word-ending-at "map," 4))
    (should= "" (get-word-ending-at "map[" 4))
    (should= "" (get-word-ending-at "map(" 4))
    (should= "" (get-word-ending-at "map}" 4)))

  (it "omits commas and other whitespace"
    (should= "foo" (get-word-ending-at ",foo" 4))
    (should= "foo" (get-word-ending-at ",foo" 4)))

  (it "includes slashes (for namespaces and classes)"
    (should= "str/split" (get-word-ending-at "str/split" 9))
    (should= "Integer/MAX_VALUE" (get-word-ending-at "Integer/MAX_VALUE" 17))))
