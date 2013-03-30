(ns reply.initialization
  (:require [clojure.pprint]
            [clojure.repl]
            [trptcolin.versioneer.core :as version]))

(defn help
  "Prints a list of helpful commands."
  []
  (println "    Exit: Control+D or (exit) or (quit)")
  (println "Commands: (user/help)")
  (println "    Docs: (doc function-name-here)")
  (println "          (find-doc \"part-of-name-here\")")
  (println "  Source: (source function-name-here)")
  (println " Javadoc: (javadoc java-object-or-class-here)")
  (println "Examples from clojuredocs.org: [clojuredocs or cdoc]")
  (println "          (user/clojuredocs name-here)")
  (println "          (user/clojuredocs \"ns-here\" \"name-here\")"))

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

(defn unresolve
  "Given a var, return a sequence of all symbols that resolve to the
  var from the current namespace *ns*."
  [var]
  (when-not (instance? clojure.lang.Var var)
    (throw (Exception. (format "unresolve: first arg must be Var"))))
  (let [home-ns (.ns var)
        sym-name-str (second (re-find #"/(.*)$" (str var)))]
    (sort-by
     #(count (str %))
     (concat
      ;; The symbols in the current namespace that map to the var, if
      ;; any
      (->> (ns-map *ns*)
           (filter (fn [[k v]] (= var v)))
           (map first))
      ;; This is the "canonical" symbol that resolves to the var, with
      ;; full namespace/symbol-name
      (list (symbol (str home-ns) sym-name-str))
      ;; There might be one or more aliases for the symbol's home
      ;; namespace defined in the current namespace.
      (->> (ns-aliases *ns*)
           (filter (fn [[ns-alias ns]] (= ns home-ns)))
           (map first)
           (map (fn [ns-alias-symbol]
                  (symbol (str ns-alias-symbol) sym-name-str))))))))

(defn better-apropos
  "Given a regular expression or stringable thing, calculate a
  sequence of all symbols in all currently-loaded namespaces such that
  it matches the str-or-pattern, with at most one such symbol per Var.
  The sequence returned contains symbols that map to those Vars, and are
  the shortest symbols that map to the Var, when qualified with the
  namespace name or alias, if that qualification is necessary to name
  the Var.  Note that it is possible the symbol returned does not match
  the str-or-pattern itself, e.g. if the symbol-to-var mapping was
  created with :rename.
  
  Searches through all non-Java symbols in the current namespace, but
  only public symbols of other namespaces."
  [str-or-pattern & opts]
  (let [matches? (if (instance? java.util.regex.Pattern str-or-pattern)
                   #(re-find str-or-pattern (str %))
                   #(.contains (str %) (str str-or-pattern)))]
    (map #(first (reply.initialization/unresolve %))
         (set
          (mapcat (fn [ns]
                    (map second
                         (filter (fn [[s v]] (matches? s))
                                 (if (= ns *ns*)
                                   (concat (ns-interns ns) (ns-refers ns))
                                   (ns-publics ns)))))
                  (all-ns))))))

(defn apro
  "Shorter-name version of apropos that also sorts and pretty-prints
  the results."
  [str-or-pattern & opts]
  (clojure.pprint/pprint (sort (apply reply.initialization/better-apropos str-or-pattern opts))))

(def clojuredocs-available?
  (delay
   (try
     (println "Loading clojuredocs-client...")
     (require '[cd-client.core])
     true
     (catch Exception e#
       (println "Warning: Could not load the ClojureDocs client, so"
                "`clojuredocs` will be unavailable")
       (println "  Details:" e# "\n")
       false))))

(defn call-with-ns-and-name
  [f v]
  (let [m (meta v)
        ns (str (.name (:ns m)))
        name (str (:name m))]
    (f ns name)))

(defmacro handle-fns-etc
  [name fn]
  (if (special-symbol? `~name)
    `(~fn "clojure.core" (str '~name))
    (let [nspace (find-ns name)]
      (if nspace
        `(println "No usage examples for namespaces as a whole like" '~name
                  "\nTry a particular symbol in a namespace,"
                  "e.g. clojure.string/join")
        `(reply.exports/call-with-ns-and-name ~fn (var ~name))))))

(defmacro lazy-clojuredocs
  "Lazily checks if the clojuredocs client is available, and uses it to
  retrieve examples if it is."
  ([v]
     `(when (deref reply.exports/clojuredocs-available?)
        (reply.exports/handle-fns-etc
         ~v (ns-resolve (symbol "cd-client.core")
                        (symbol "pr-examples-core")))))
  ([ns-str var-str]
     `(when (deref reply.exports/clojuredocs-available?)
        ((ns-resolve (symbol "cd-client.core") (symbol "pr-examples-core"))
         ~ns-str ~var-str))))

(defn formify-file [f]
  (read-string (str "(do " (slurp f) ")")))

(defn default-init-code
  "Assumes cd-client will be on the classpath when this is evaluated."
  []
  `(do
     (println "REPL-y" ~(version/get-version "reply" "reply"))
     (println "Clojure" (clojure-version))

     (use '[clojure.repl :only ~'[source apropos dir]])
     ; doc and find-doc live in clojure.core in 1.2
     (when (ns-resolve '~'clojure.repl '~'pst)
       (refer 'clojure.repl :only '~'[pst doc find-doc]))

     (use '[clojure.java.javadoc :only ~'[javadoc]])
     (use '[clojure.pprint :only ~'[pp pprint]])

     (create-ns 'reply.exports)
     (~'intern '~'reply.exports '~'original-ns ~'*ns*)
     (in-ns '~'reply.exports)
     (clojure.core/refer '~'clojure.core)

     ~(export-definition 'reply.initialization/intern-with-meta)

     ~(export-definition 'reply.initialization/help)
     (~'intern-with-meta '~'user '~'help ~'#'help)

     ~(export-definition 'reply.initialization/apro)
     ~(export-definition 'reply.initialization/better-apropos)
     (~'intern-with-meta '~'user '~'apro ~'#'apro)
     (~'intern-with-meta '~'user '~'better-apropos ~'#'better-apropos)

     ~(export-definition 'reply.initialization/clojuredocs-available?)
     ~(export-definition 'reply.initialization/call-with-ns-and-name)
     ~(export-definition 'reply.initialization/handle-fns-etc)
     ~(export-definition 'reply.initialization/lazy-clojuredocs)
     (~'intern-with-meta '~'user '~'clojuredocs ~'#'lazy-clojuredocs)
     (~'intern-with-meta '~'user '~'cdoc ~'#'lazy-clojuredocs)

     (try
       (require '[complete.core])
       ; hack for 1.2 support until we release the next clojure-complete version
       ~(export-definition 'reply.initialization/resolve-class)
       (~'reply.exports/intern-with-meta
          '~'complete.core '~'resolve-class ~'#'resolve-class)

       (catch Exception e#
         (try
           (eval '~(formify-file
                     (ClassLoader/getSystemResource "complete/core.clj")))
           (catch Exception f#
             (intern (create-ns '~'complete.core) '~'completions
                     (fn [prefix# ns#] []))
             (println "Unable to initialize completions.")))))

     (in-ns (ns-name ~'reply.exports/original-ns))

     (~'user/help)
     nil))

(defn eval-in-user-ns [code]
  (let [original-ns (symbol (str *ns*))]
    (in-ns 'user)
    (let [result (eval code)]
      (in-ns original-ns)
      result)))

(defn construct-init-code
  [{:keys [skip-default-init
           custom-init custom-eval] :as options}]
  `(do
    ~(when-not skip-default-init (default-init-code))
     ~(when custom-eval custom-eval)
     ~(when custom-init custom-init)
    nil))

