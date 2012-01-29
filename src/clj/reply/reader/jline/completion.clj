(ns reply.reader.jline.completion
  (:require [reply.completion :as completion]
            [reply.eval-state :as eval-state]
            [complete :as ninjudd.complete])
  (:import [jline.console.completer Completer]))

(defn make-completer []
  (proxy [Completer] []
    (complete [^String buffer cursor ^java.util.List candidates]
      (let [buffer (or buffer "")
            prefix (or (completion/get-word-ending-at buffer cursor) "")
            prefix-length (.length prefix)
            possible-completions (sort (ninjudd.complete/completions prefix (eval-state/get-ns)))]
        (if (or (empty? possible-completions) (zero? prefix-length))
          -1
          (do
            (.addAll candidates possible-completions)
            (- cursor prefix-length)))))))

