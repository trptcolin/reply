(ns reply.reader.jline-completion-test
  (:require [reply.reader.jline.completion :as c]
            [clojure.test :as t]))

(t/deftest construct-possible-completions-form
  (t/testing "does correct quoting in completions form request"
    (t/is (= '(incomplete.core/completions (str "clojure.core/map-")
                                           (symbol "user"))
             (c/construct-possible-completions-form "clojure.core/map-"
                                                    "user")))))

(t/deftest completer
  (let [times (atom 0)
        completer (c/make-completer eval
                                    #(swap! times inc)
                                    "clojure.core")]
    (t/testing "populates the list with possible completions"
      (let [candidates (java.util.LinkedList.)
            word-start (.complete completer " (map" 5 candidates)]
        (t/is (= ["map" "map-indexed" "map?" "mapcat" "mapv"]
                 candidates))
        (t/is (= 1 @times))
        (t/is (= 2 word-start)))))

  (let [times (atom 0)
        completer (c/make-completer eval
                                    #(swap! times inc)
                                    "clojure.core")]
    (t/testing "gets empty completions when none exist"
      (let [candidates (java.util.LinkedList.)
            word-start (.complete completer "foobar" 6 candidates)]
        (t/is (= [] candidates))
        (t/is (= 0 @times))
        (t/is (= -1 word-start)))))

  (let [times (atom 0)
        completer (c/make-completer eval
                                    #(swap! times inc)
                                    "clojure.core")]
    (t/testing "handles a single slash"
      (let [candidates (java.util.LinkedList.)
            word-start (.complete completer "/" 1 candidates)]
        (t/is (= [] candidates))
        (t/is (= 0 @times))
        (t/is (= -1 word-start)))))

  (let [times (atom 0)
        completer (c/make-completer eval
                                    #(swap! times inc)
                                    "clojure.core")]
    (t/testing "handles null buffer"
      (let [candidates (java.util.LinkedList.)
            word-start (.complete completer nil 6 candidates)]
        (t/is (= [] candidates))
        (t/is (= 0 @times))
        (t/is (= -1 word-start)))))

  (let [times (atom 0)
        completer (c/make-completer eval
                                    #(swap! times inc)
                                    "clojure.core")]
    (t/testing "handles no word at the end"
      (let [candidates (java.util.LinkedList.)
            word-start (.complete completer "map " 4 candidates)]
        (t/is (= [] candidates))
        (t/is (= 0 @times))
        (t/is (= -1 word-start))))))
