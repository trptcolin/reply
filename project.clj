(defproject reply "0.6.0-SNAPSHOT"
  :description "REPL-y: A fitter, happier, more productive REPL for Clojure."
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [jline "2.14.6"]
                 [clj-stacktrace "0.2.8"]
                 [nrepl "1.5.2"]
                 [org.clojure/tools.cli "1.3.250"]
                 [nrepl/drawbridge "0.3.0"]
                 [trptcolin/versioneer "0.2.0"]
                 [org.nrepl/incomplete "0.1.0"]
                 [org.clojars.trptcolin/sjacket "0.1.4"
                  :exclusions [org.clojure/clojure]]
                 ;; bump transitive dep to avoid compatibility warning
                 [net.cgrand/parsley "0.9.3" :exclusions [org.clojure/clojure]]]
  :min-lein-version "2.9.1"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/trptcolin/reply"
  :profiles {:dev {:dependencies [[classlojure "0.6.6"]]}
             :base {:dependencies []}
             ;; Clojure versions matrix
             :provided {:dependencies [[org.clojure/clojure "1.12.4"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
             :1.12 {:dependencies [[org.clojure/clojure "1.12.4"]]}}
  ;;    :jvm-opts ["-Djline.internal.Log.trace=true"]
  :aot [reply.reader.jline.JlineInputReader]
  :main ^{:skip-aot true} reply.main
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases true}]])
