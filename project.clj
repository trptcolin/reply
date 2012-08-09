(let [dev-deps '[[midje "1.4.0" :exclusions [org.clojure/clojure]]]]

  (defproject reply "0.1.0-SNAPSHOT"
    :description "REPL-y: A fitter, happier, more productive REPL for Clojure."
    :dependencies [[org.clojure/clojure "1.4.0"]
                   [org.clojars.trptcolin/jline "2.8-alpha1"]
                   [org.thnetos/cd-client "0.3.4"]
                   [clj-stacktrace "0.2.4"]
                   [org.clojure/tools.nrepl "0.2.0-beta8"]
                   [org.clojure/tools.cli "0.2.1"]
                   [com.cemerick/drawbridge "0.0.6"]
                   [trptcolin/versioneer "0.1.0"]
                   [clojure-complete "0.2.1"]]
    :profiles {:dev {:dependencies ~dev-deps}}
    :dev-dependencies ~dev-deps
    :aot [reply.reader.jline.JlineInputReader]
    :source-path "src/clj"
    :java-source-path "src/java"
    :source-paths ["src/clj"]
    :java-source-paths ["src/java"]
    :main ^{:skip-aot true} reply.main))
