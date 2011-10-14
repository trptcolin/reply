(defproject reply "0.0.1-SNAPSHOT"
  :description "REPL-y: a better Clojure REPL"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.scala-lang/jline "2.9.1"]]
  :aot [reply.JlineInputReader]
  :javac-options {:destdir "classes/"}
  :java-source-path "src")
