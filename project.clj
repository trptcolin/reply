(load-file "src/clj/version.clj")

(defproject reply version/*reply-version*
  :description "REPL-y: A fitter, happier, more productive REPL for Clojure." 
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojars.trptcolin/jline "2.7-alpha5"]
                 [org.thnetos/cd-client "0.3.4"]
                 [clj-stacktrace "0.2.4"]
                 [org.clojure/tools.nrepl "0.2.0-beta6"]
                 [com.cemerick/drawbridge "0.0.3"]
                 [clojure-complete "0.2.1"]]
  :dev-dependencies [[midje "1.3-alpha4" :exclusions [org.clojure/clojure]]
                     [lein-midje "[1.0.0,)"]]
  :aot [reply.reader.jline.JlineInputReader]
  :source-path "src/clj"
  :java-source-path "src/java"
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :main ^{:skip-aot true} reply.main)
