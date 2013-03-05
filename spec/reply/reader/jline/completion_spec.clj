(ns reply.reader.jline.completion-spec
  (:use [reply.reader.jline.completion]
        [speclj.core]))

(describe "construct-possible-completions-form"
  (it "does correct quoting in completions form request"
    (should= '(complete.core/completions (str "clojure.core/map-") (symbol "user"))
             (construct-possible-completions-form "clojure.core/map-" "user"))))

