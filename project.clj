(defproject reply "0.0.2-SNAPSHOT"
  :description "REPL-y: A fitter, happier, more productive REPL for Clojure."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.scala-lang/jline "2.9.1"]]
  :aot [reply.JlineInputReader]
  :javac-options {:destdir "classes/"}
  :java-source-path "src")
