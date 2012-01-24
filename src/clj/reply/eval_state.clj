(ns reply.eval-state
  (:refer-clojure :exclude [with-bindings]))

(def bindings
  (atom
    {#'clojure.core/*ns* (the-ns 'user)
     #'clojure.core/*warn-on-reflection* *warn-on-reflection*
     #'clojure.core/*math-context* *math-context*
     #'clojure.core/*print-meta* *print-meta*
     #'clojure.core/*print-length* *print-length*
     #'clojure.core/*print-level* *print-level*
     #'clojure.core/*compile-path* (System/getProperty "clojure.compile.path" "classes")
     #'clojure.core/*command-line-args* *command-line-args*
     #'clojure.core/*unchecked-math* *unchecked-math*
     #'clojure.core/*assert* *assert*
     #'clojure.core/*1 "foo"
     #'clojure.core/*2 nil
     #'clojure.core/*3 nil
     #'clojure.core/*e nil}))

(defn get-ns []
  (@bindings #'*ns*))

(defn set-bindings! []
  (doseq [k (keys @bindings)]
    (swap! bindings assoc k (deref k))))

(defn with-bindings [f]
  (with-bindings* @bindings
    (fn []
      (try
        (let [result (f)]
          (set! *3 *2)
          (set! *2 *1)
          (set! *1 result)
          (set-bindings!)
          result)
        (catch Throwable e
          (set! *e e)
          (set-bindings!)
          (throw e))))))

