(ns reply.initialization
  (:require [clojure.pprint]
            [clojure.repl]))

(defmacro repl-defn [sym & args]
  (let [no-meta-source (with-out-str (clojure.pprint/pprint `(defn ~sym ~@args)))
        meta-source `(clojure.core/defn ~(with-meta sym {:source no-meta-source}) ~@args)]
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

(defn default-init-code []
  `(do
    (println "Welcome to REPL-y!")
    (println "Clojure" (clojure-version))
    (use '[clojure.repl :only ~'[source apropos dir pst doc find-doc]])
    (use '[clojure.java.javadoc :only ~'[javadoc]])
    (use '[clojure.pprint :only ~'[pp pprint]])

    ~(export-definition 'reply.initialization/help)

    (ns reply.exports)
    ~(export-definition 'reply.initialization/intern-with-meta)

    (binding [*err* (java.io.StringWriter.)]
      ~(export-definition 'reply.initialization/repl-defn)
      (~'intern-with-meta '~'user '~'defn #'repl-defn))

    ~(export-definition 'reply.initialization/sourcery)
    (~'intern-with-meta '~'user '~'sourcery #'sourcery)

    (in-ns '~'user)

    ; assumes cd-client is on the execution classpath by now
    (require '[cd-client.core])
    (~'reply.exports/intern-with-meta '~'user '~'clojuredocs #'cd-client.core/pr-examples)

    (help)
    nil))

(defn eval-in-user-ns [code]
  (let [original-ns (symbol (str *ns*))]
    (in-ns 'user)
    (eval code)
    (in-ns original-ns)))

(defn construct-init-code
  [{:keys [skip-default-init
           custom-init] :as options}]
  `(do
    ~(when-not skip-default-init (default-init-code))
    ~(when custom-init custom-init)
    nil))

