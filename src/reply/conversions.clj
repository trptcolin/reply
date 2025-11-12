(ns reply.conversions)

(defn ->fn [config default]
  (cond (fn? config) config
        (seq? config) (eval config)
        (symbol? config) (eval config)
        :else default))
