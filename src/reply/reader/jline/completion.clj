(ns reply.reader.jline.completion
  (:require [reply.completion :as completion]
            [complete :as ninjudd.complete])
  (:import [scala.tools.jline.console.completer Completer]
           [scala.tools.jline.console ConsoleReader]
           [scala.tools.jline.console.completer
             CandidateListCompletionHandler
             Completer]))

(defn make-completion-handler []
  (proxy [CandidateListCompletionHandler] []
    (complete [^ConsoleReader reader ^java.util.List candidates pos]
      (let [buf (.getCursorBuffer reader)]
        (if (= 1 (.size candidates))
          (let [value (.get candidates 0)]
            (if (= value (.toString buf))
              false
              (do (CandidateListCompletionHandler/setBuffer
                    reader
                    value
                    pos)
                  true)))
          (do
            (when (> (.size candidates) 1)
              (CandidateListCompletionHandler/setBuffer
                reader
                (completion/get-unambiguous-completion
                  candidates)
                pos))
            (CandidateListCompletionHandler/printCandidates
              reader
              candidates)
            (.redrawLine reader)
            true))))))

(defn make-completer [completions]
  (proxy [Completer] []
    (complete [^String buffer cursor ^java.util.List candidates]
      (let [buffer (or buffer "")
            prefix (or (completion/get-word-ending-at buffer cursor) "")
            prefix-length (.length prefix)
            possible-completions (sort (ninjudd.complete/completions prefix))]
        (if (or (empty? possible-completions) (zero? prefix-length))
          -1
          (do
            (.addAll candidates possible-completions)
            (- cursor prefix-length)))))))

(defn make-var-completer [ns]
  (let [collections (concat (map keys ((juxt ns-publics
                                             ns-refers
                                             ns-imports)
                                       ns))
                            [(map str (all-ns))])]
    (make-completer
      (mapcat (comp sort (partial map str))
              collections))))

