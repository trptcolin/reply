(ns reply.hacks.complete
  (:use [clojure.main :only [repl-exception]]))

(defn resolve-class [sym]
  (try (let [val (resolve sym)]
    (when (class? val) val))
      (catch RuntimeException e
        (when (not= ClassNotFoundException (class (repl-exception e)))
          (throw e)))))
