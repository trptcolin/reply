(ns reply.initialization)

(defn help
  "Prints a list of helpful commands."
  []
  (println "    Exit: Control+D or (exit) or (quit)")
  (println "Commands: (help)")
  (println "    Docs: (doc function-name-here)")
  (println "          (find-doc \"part-of-name-here\")")
  (println "  Source: (source function-name-here)")
  (println " Javadoc: (javadoc java-object-or-class-here)")
  (println "Examples from clojuredocs.org:")
  (println "          (clojuredocs name-here)")
  (println "          (clojuredocs \"ns-here\" \"name-here\")"))

(defn intern-with-meta [ns sym value-var]
  (intern ns
          (with-meta sym (assoc (meta value-var) :ns (the-ns ns)))
          @value-var))

(def default-init-code
  '(do
    (println "Welcome to REPL-y!")
    (println "Clojure" (clojure-version))
    (use '[clojure.repl :only (source apropos dir pst doc find-doc)])
    (use '[clojure.java.javadoc :only (javadoc)])
    (use '[clojure.pprint :only (pp pprint)])
    (require '[cd-client.core])
    (def exit reply.main/exit)
    (def quit reply.main/exit)
    (def help reply.initialization/help)
    (help)
    (reply.initialization/intern-with-meta 'user 'clojuredocs #'cd-client.core/pr-examples)
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
    ~(when-not skip-default-init default-init-code)
    ~(when custom-init custom-init)
    nil))

