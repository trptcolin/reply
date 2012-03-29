(ns reply.initialization
  (:require [clojure.pprint]
            [clojure.repl]))

(defmacro repl-defn [sym & args]
  (let [no-meta-source (binding [*print-meta* true]
                         (with-out-str (clojure.pprint/pprint `(defn ~sym ~@args))))
        meta-source `(clojure.core/defn ~(vary-meta sym assoc :source no-meta-source) ~@args)]
    meta-source))

(defmacro sourcery [name]
  `(if-let [s# (:source (meta (var ~name)))]
    (do (print s#) (flush))
    (clojure.repl/source ~name)))

(defn help
  "Prints a list of helpful commands."
  []
  (println "    Exit: Control+D or (exit) or (quit)")
  (println "Commands: (help)")
  (println "    Docs: (doc function-name-here)")
  (println "          (find-doc \"part-of-name-here\")")
  (println "  Source: (source function-name-here)")
  (println "          (sourcery function-name-here)")
  (println " Javadoc: (javadoc java-object-or-class-here)")
  (println "Examples from clojuredocs.org:")
  (println "          (clojuredocs name-here)")
  (println "          (clojuredocs \"ns-here\" \"name-here\")"))

(defn intern-with-meta [ns sym value-var]
  (intern ns
          (with-meta sym (meta value-var))
          @value-var))

(defn export-definition [s]
  (read-string (clojure.repl/source-fn s)))

(def resolve-class
  (fn [sym]
    (try (let [val (resolve sym)]
      (when (class? val) val))
        (catch Exception e
          (when (not= ClassNotFoundException
                      (class (clojure.main/repl-exception e)))
            (throw e))))))


(defn default-init-code
  "Assumes cd-client will be on the classpath when this is evaluated."
  []
  `(do
    (println "Welcome to REPL-y!")
    (println "Clojure" (clojure-version))

    (use '[clojure.repl :only ~'[source apropos dir]])
    ; doc and find-doc live in clojure.core in 1.2
    (when (ns-resolve '~'clojure.repl '~'pst)
      (refer 'clojure.repl :only '~'[pst doc find-doc]))

    (use '[clojure.java.javadoc :only ~'[javadoc]])
    (use '[clojure.pprint :only ~'[pp pprint]])

    ~(export-definition 'reply.initialization/help)
    ~(export-definition 'reply.main/exit)
    (def ~'quit ~'exit)

    (ns reply.exports)
    ~(export-definition 'reply.initialization/intern-with-meta)

    (~'intern-with-meta '~'user '~'quit #'user/exit)

    (binding [*err* (java.io.StringWriter.)]
      ~(export-definition 'reply.initialization/repl-defn)
      (~'intern-with-meta '~'user '~'defn ~'#'repl-defn))

    ~(export-definition 'reply.initialization/sourcery)
    (~'intern-with-meta '~'user '~'sourcery ~'#'sourcery)

    (require '[complete.core])
    ~(export-definition 'reply.initialization/resolve-class)
    (~'intern-with-meta '~'complete.core '~'resolve-class ~'#'resolve-class)

    (in-ns '~'user)

    (try
      (require '[cd-client.core])
      (let [pr-exes# (ns-resolve '~'cd-client.core '~'pr-examples)]
        (~'reply.exports/intern-with-meta '~'user '~'clojuredocs pr-exes#))
      (catch Exception e#
        (println "Warning: Could not load the ClojureDocs client, so `clojuredocs` will be unavailable")
        (println "  Details:" e# "\n")))

    (~'help)
    nil))

(defn eval-in-user-ns [code]
  (let [original-ns (symbol (str *ns*))]
    (in-ns 'user)
    (let [result (eval code)]
      (in-ns original-ns)
      result)))

(defn construct-init-code
  [{:keys [skip-default-init
           custom-init] :as options}]
  `(do
    ~(when-not skip-default-init (default-init-code))
    ~(when custom-init custom-init)
    nil))

