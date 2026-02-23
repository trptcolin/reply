(ns reply.reader.jline.completion-test
  (:require [reply.reader.jline.completion :as completion]
            [clojure.test :as t])
  (:import [org.jline.reader Candidate ParsedLine]))

(defn- fake-parsed-line [word]
  (reify ParsedLine
    (word [_] word)
    (wordIndex [_] 0)
    (wordCursor [_] 0)
    (cursor [_] 0)
    (line [_] word)
    (words [_] [])))

(defn- candidate-values [^java.util.List candidates]
  (mapv #(.value ^Candidate %) candidates))

(t/deftest construct-possible-completions-form
  (t/testing "does correct quoting in completions form request"
    (t/is (= '(incomplete.core/completions (str "clojure.core/map-") (symbol "user"))
             (completion/construct-possible-completions-form "clojure.core/map-" "user")))))

(t/deftest completer-integration
  (let [completer (completion/make-completer eval "clojure.core")]

    (t/testing "populates the list with possible completions"
      (let [candidates (java.util.LinkedList.)]
        (.complete completer nil (fake-parsed-line "map") candidates)
        (t/is (= ["map" "map-entry?" "map-indexed" "map?" "mapcat" "mapv"]
                 (candidate-values candidates)))))

    (t/testing "gets empty completions when none exist"
      (let [candidates (java.util.LinkedList.)]
        (.complete completer nil (fake-parsed-line "foobar") candidates)
        (t/is (empty? candidates))))

    (t/testing "handles a single slash"
      (let [candidates (java.util.LinkedList.)]
        (.complete completer nil (fake-parsed-line "/") candidates)
        (t/is (empty? candidates))))

    (t/testing "handles null buffer"
      (let [candidates (java.util.LinkedList.)]
        (.complete completer nil (fake-parsed-line "") candidates)
        (t/is (empty? candidates))))

    (t/testing "handles no word at the end"
      (let [candidates (java.util.LinkedList.)]
        (.complete completer nil (fake-parsed-line "") candidates)
        (t/is (empty? candidates))))))
