(ns reply.initialization
  (:require [clojure.pprint]))

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

(defn default-init-code [no-clojuredocs?]
  (list 'do
        '(println "Welcome to REPL-y!")
        '(println "Clojure" (clojure-version))
        '(use '[clojure.repl :only (source apropos dir pst doc find-doc)])
        '(use '[clojure.java.javadoc :only (javadoc)])
        '(use '[clojure.pprint :only (pp pprint)])
        (when-not no-clojuredocs?
          '(require '[cd-client.core]))
        '(def exit reply.main/exit)
        '(def quit reply.main/exit)
        '(def help reply.initialization/help)

        '(help)
        (when-not no-clojuredocs?
          '(reply.initialization/intern-with-meta 'user 'clojuredocs #'cd-client.core/pr-examples))
        '(binding [*err* (java.io.StringWriter.)]
           (reply.initialization/intern-with-meta 'user 'defn #'reply.initialization/repl-defn))
        '(reply.initialization/intern-with-meta 'user 'sourcery #'reply.initialization/sourcery)
        nil))

(defn eval-in-user-ns [code]
  (let [original-ns (symbol (str *ns*))]
    (in-ns 'user)
    (eval code)
    (in-ns original-ns)))

(defn construct-init-code
  [{:keys [skip-default-init
           custom-init
           no-clojuredocs] :as options}]
  `(do
     ~(when-not skip-default-init (default-init-code no-clojuredocs))
     ~(when custom-init custom-init)
     nil))

