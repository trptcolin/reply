(ns reply.reader.jline.completion
  (:require [reply.completion :as completion]
            [complete :as ninjudd.complete])
  (:import [jline.console.completer Completer]))

(defn make-completer [ns]
  (proxy [Completer] []
    (complete [^String buffer cursor ^java.util.List candidates]
      (let [buffer (or buffer "")
            prefix (or (completion/get-word-ending-at buffer cursor) "")
            prefix-length (.length prefix)
            possible-completions (sort (ninjudd.complete/completions prefix ns))]
        (if (or (empty? possible-completions) (zero? prefix-length))
          -1
          (do
            (.addAll candidates possible-completions)
            (- cursor prefix-length)))))))

