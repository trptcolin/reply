(ns reply.hacks.complete
  (:require [clojure.main]))

(defn resolve-class [sym]
  (try (let [val (resolve sym)]
    (when (class? val) val))
      (catch RuntimeException e
        (when (not= ClassNotFoundException
                    (class (clojure.main/repl-exception e)))
          (throw e)))))
