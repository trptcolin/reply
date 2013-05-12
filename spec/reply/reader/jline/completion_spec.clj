(ns reply.reader.jline.completion-spec
  (:use [reply.reader.jline.completion]
        [speclj.core]))

(describe "construct-possible-completions-form"
  (it "does correct quoting in completions form request"
    (should= '(complete.core/completions (str "clojure.core/map-") (symbol "user"))
             (construct-possible-completions-form "clojure.core/map-" "user"))))

(describe "using the completer"

  (with redraw-count (atom 0))
  (with completer (make-completer eval
                                  #(swap! @redraw-count inc)
                                  "clojure.core"))

  (it "populates the list with possible completions"
    (let [candidates (java.util.LinkedList.)
          word-start (.complete @completer " (map" 5 candidates)]
      (should= ["map" "map-indexed" "map?" "mapcat" "mapv"]
               candidates)
      (should= 1 @@redraw-count)
      (should= 2 word-start)))

  (it "gets empty completions when none exist"
    (let [candidates (java.util.LinkedList.)
          word-start (.complete @completer "foobar" 6 candidates)]
      (should= [] candidates)
      (should= 0 @@redraw-count)
      (should= -1 word-start)))

  (it "handles null buffer"
    (let [candidates (java.util.LinkedList.)
          word-start (.complete @completer nil 6 candidates)]
      (should= [] candidates)
      (should= 0 @@redraw-count)
      (should= -1 word-start)))

  (it "handles no word at the end"
    (let [candidates (java.util.LinkedList.)
          word-start (.complete @completer "map " 4 candidates)]
      (should= [] candidates)
      (should= 0 @@redraw-count)
      (should= -1 word-start))))
