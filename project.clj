(defproject reply "0.1.0-SNAPSHOT"
  :description "REPL-y: A fitter, happier, more productive REPL for Clojure."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojars.trptcolin/jline "2.7-alpha1"]
                 [org.thnetos/cd-client "0.3.1" :exclusions [org.clojure/clojure]]
                 [clj-stacktrace "0.2.4"]
                 [clojure-complete "0.2.1" :exclusions [org.clojure/clojure]]
                 [org.clojure/tools.nrepl "0.2.0-beta1"]]
  :dev-dependencies [[midje "1.3-alpha4" :exclusions [org.clojure/clojure]]
                     [lein-midje "[1.0.0,)"]]
  :aot [reply.reader.jline.JlineInputReader]
  :source-path "src/clj"
  :java-source-path "src/java")
