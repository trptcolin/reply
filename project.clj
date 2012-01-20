(defproject reply "0.1.0-SNAPSHOT"
  :description "REPL-y: A fitter, happier, more productive REPL for Clojure."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [net.sf.jline/jline "2.6-SNAPSHOT"]
                 [clojure-complete "0.1.4"]]
  :dev-dependencies [[midje "1.3-alpha4"]
                     [lein-midje "[1.0.0,)"]]
  :aot [reply.reader.jline.JlineInputReader]
  :repositories {"sonatype"
                 {:url "http://oss.sonatype.org/content/repositories/snapshots"}}
  :java-source-path "src")
