(defproject reply "0.1.0-SNAPSHOT"
  :description "REPL-y: A fitter, happier, more productive REPL for Clojure."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojars.trptcolin/jline "2.6-SNAPSHOT"]
                 [org.thnetos/cd-client "0.3.1" :exclusions [org.clojure/clojure]]
                 [clj-stacktrace "0.2.4"]
                 [clojure-complete "0.2.0"]
                 [org.clojure/tools.nrepl "0.0.6-SNAPSHOT"]]
  :dev-dependencies [[midje "1.3-alpha4" :exclusions [org.clojure/clojure]]
                     [lein-midje "[1.0.0,)"]]
  :repositories {
    "sonatype" {:url "http://oss.sonatype.org/content/repositories/snapshots" } }
  :aot [reply.reader.jline.JlineInputReader]
  :source-path "src/clj"
  :java-source-path "src/java")
