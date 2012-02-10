(ns reply.reader.jline.completion-test
  (:use [reply.reader.jline.completion]
        [midje.sweet]))

(fact "correct quoting in completions form request"
  (construct-possible-completions-form "clojure.core/map-") =>
    '(do (require '[complete :as ninjudd.complete])
         (sort (ninjudd.complete/completions (str "clojure.core/map-") *ns*)))
)
