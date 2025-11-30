(ns reply.completion-test
  (:require [reply.completion :as completion]
            [clojure.test :as t]))

(t/deftest get-word-ending-at
  (t/testing "falls back to the last location if the index is too high"
    (t/is (= "map" (completion/get-word-ending-at "map" 20))))

  (t/testing "finds no words when there's nothing there"
    (t/is (= "" (completion/get-word-ending-at "" 0)))
    (t/is (= "" (completion/get-word-ending-at " " 0)))
    (t/is (= "" (completion/get-word-ending-at "map" 0))))

  (t/testing "finds a plain symbol"
    (t/is (= "ma" (completion/get-word-ending-at "map" 2)))
    (t/is (= "map" (completion/get-word-ending-at "map" 3)))
    (t/is (= "map" (completion/get-word-ending-at "(map first [0 1 2])" 4))))

  (t/testing "finds namespaces, also as symbols"
    (t/is (= "clojure.c" (completion/get-word-ending-at "clojure.c" 9)))
    (t/is (= "clojure.c" (completion/get-word-ending-at "'clojure.c" 10))))

  (t/testing "finds keywords"
    (t/is (= ":clojure.c" (completion/get-word-ending-at ":clojure.c" 10)))
    (t/is (= ":ke" (completion/get-word-ending-at ":keyword" 3)))
    (t/is (= "::foobar" (completion/get-word-ending-at "::foobar" 8))))

  (t/testing "omits brackets of several sorts (but takes asterisks)"
    (t/is (= "*foo" (completion/get-word-ending-at "(*foo" 5)))
    (t/is (= "*foo" (completion/get-word-ending-at "[*foo" 5)))
    (t/is (= "*foo" (completion/get-word-ending-at "{*foo" 5)))
    (t/is (= "foo" (completion/get-word-ending-at "]foo" 4)))
    (t/is (= "foo" (completion/get-word-ending-at "}foo" 4)))
    (t/is (= "foo" (completion/get-word-ending-at ")foo" 4))))

  (t/testing "gets nothing when the last segment is a non-word"
    (t/is (= "" (completion/get-word-ending-at "map " 4)))
    (t/is (= "" (completion/get-word-ending-at "map," 4)))
    (t/is (= "" (completion/get-word-ending-at "map[" 4)))
    (t/is (= "" (completion/get-word-ending-at "map(" 4)))
    (t/is (= "" (completion/get-word-ending-at "map}" 4))))

  (t/testing "omits commas and other whitespace"
    (t/is (= "foo" (completion/get-word-ending-at ",foo" 4)))
    (t/is (= "foo" (completion/get-word-ending-at ",foo" 4))))

  (t/testing "includes slashes (for namespaces and classes)"
    (t/is (= "str/split" (completion/get-word-ending-at "str/split" 9)))
    (t/is (= "Integer/MAX_VALUE" (completion/get-word-ending-at
                                  "Integer/MAX_VALUE" 17)))))
