(defproject reply "0.0.2-SNAPSHOT"
  :description "REPL-y: A fitter, happier, more productive REPL for Clojure."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.scala-lang/jline "2.9.1"]
                 [clojure-complete "0.1.4"]]
  :dev-dependencies [[midje "1.3-alpha4"]
                     [lein-midje "[1.0.0,)"]]
  :aot [reply.reader.jline.JlineInputReader]
  :java-source-path "src")
