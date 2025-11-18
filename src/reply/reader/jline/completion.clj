(ns reply.reader.jline.completion
  (:require [reply.completion :as completion]
            [incomplete.core])
  (:import (org.jline.reader Completer
                             ParsedLine)))

(defn construct-possible-completions-form [prefix ns]
  `(~'incomplete.core/completions (~'str ~prefix) (~'symbol ~ns)))

(defn make-completer [eval-fn redraw-line-fn ns]
  (proxy [Completer] []
    (complete [_reader ^ParsedLine line ^java.util.List candidates]
      (let [prefix ^String (.wordCursor line)
            prefix-length (.length prefix)]
        (if (zero? prefix-length)
          -1
          (let [possible-completions-form (construct-possible-completions-form
                                            prefix ns)
                possible-completions (eval-fn possible-completions-form)]
            (if (empty? possible-completions)
              -1
              (do
                (.addAll candidates (map :candidate possible-completions))
                (redraw-line-fn)))))))))
