(ns reply.reader.jline.completion
  (:require [reply.completion :as completion]
            [incomplete.core])
  (:import [org.jline.reader Completer Candidate ParsedLine LineReader]))

(defn construct-possible-completions-form [prefix ns]
  `(~'incomplete.core/completions (~'str ~prefix) (~'symbol ~ns)))

(defn get-prefix [^ParsedLine line]
  (let [word (.word line)]
    (or word "")))

(defn make-completer [eval-fn ns]
  (proxy [Completer] []
    (complete [^LineReader reader ^ParsedLine line ^java.util.List candidates]
      (let [prefix ^String (get-prefix line)
            prefix-length (.length prefix)]
        (when-not (zero? prefix-length)
          (let [possible-completions-form (construct-possible-completions-form
                                            prefix ns)
                possible-completions (eval-fn possible-completions-form)]
            (when-not (empty? possible-completions)
              (doseq [c possible-completions]
                (.add candidates (Candidate. (:candidate c)))))))))))
