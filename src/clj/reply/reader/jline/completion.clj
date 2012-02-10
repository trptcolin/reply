(ns reply.reader.jline.completion
  (:require [reply.completion :as completion]
            [reply.eval-state :as eval-state]
            [complete :as ninjudd.complete])
  (:import [jline.console.completer Completer]))

(defn construct-possible-completions-form [prefix]
  `(do
    (~'require '[~'complete :as ~'ninjudd.complete])
    (~'sort (~'ninjudd.complete/completions (~'str ~prefix) ~'*ns*))))

(defn make-completer [eval-fn]
  (proxy [Completer] []
    (complete [^String buffer cursor ^java.util.List candidates]
      (let [buffer (or buffer "")
            prefix (or (completion/get-word-ending-at buffer cursor) "")
            prefix-length (.length prefix)
            possible-completions-form (construct-possible-completions-form prefix)
            possible-completions (eval-fn possible-completions-form)]
        (if (or (empty? possible-completions) (zero? prefix-length))
          -1
          (do
            (.addAll candidates possible-completions)
            (- cursor prefix-length)))))))

