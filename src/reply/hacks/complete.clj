(ns reply.hacks.complete
  (:use [clojure.main :only [repl-exception]]))

(defn resolve-class [sym]
  (try (let [val (resolve sym)]
    (when (class? val) val))
      (catch RuntimeException e
        (if (= ClassNotFoundException (type (repl-exception e)))
          nil
          (throw e)))))
