(ns reply.eval-state)

(def bindings (atom {}))

(defn get-ns []
  (or (@bindings '*ns*) *ns*))

(defn set-ns []
  (swap! bindings assoc '*ns* *ns*))
