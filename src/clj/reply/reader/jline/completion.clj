(ns reply.reader.jline.completion
  (:require [reply.completion :as completion]
            [complete.core])
  (:import [jline.console.completer Completer]))

(defn construct-possible-completions-form [prefix ns]
  `(~'complete.core/completions (~'str ~prefix) (~'symbol ~ns)))

(defn get-prefix [buffer cursor]
  (let [buffer (or buffer "")]
    (or (completion/get-word-ending-at buffer cursor) "")))

(defn make-completer [eval-fn redraw-line-fn ns]
  (proxy [Completer] []
    (complete [^String buffer cursor ^java.util.List candidates]
      (let [prefix ^String (get-prefix buffer cursor)
            prefix-length (.length prefix)]
        (if (zero? prefix-length)
          -1
          (let [possible-completions-form (construct-possible-completions-form
                                            prefix ns)
                possible-completions (eval-fn possible-completions-form)]
            (if (empty? possible-completions)
              -1
              (do
                (.addAll candidates (map str possible-completions))
                (redraw-line-fn)
                (- cursor prefix-length)))))))))
