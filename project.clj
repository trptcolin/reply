(let [dev-deps '[[speclj "2.7.2"]
                 [classlojure "0.6.6"]]]

  (defproject reply "0.3.0-SNAPSHOT"
    :description "REPL-y: A fitter, happier, more productive REPL for Clojure."
    :dependencies [[org.clojure/clojure "1.4.0"]
                   [jline "2.11"]
                   [org.thnetos/cd-client "0.3.6"]
                   [clj-stacktrace "0.2.4"]
                   [org.clojure/tools.nrepl "0.2.3"]
                   [org.clojure/tools.cli "0.2.1"]
                   [com.cemerick/drawbridge "0.0.6"]
                   [trptcolin/versioneer "0.1.0"]
                   [clojure-complete "0.2.3"]
                   [org.clojars.trptcolin/sjacket "0.1.0.3"
                    :exclusions [org.clojure/clojure]]]
    :profiles {:dev {:dependencies ~dev-deps}}
    :dev-dependencies ~dev-deps
    :plugins ~dev-deps
    :source-path "src/clj"
    :java-source-path "src/java"
    :test-path "spec"
    :source-paths ["src/clj"]
    :java-source-paths ["src/java"]
;    :jvm-opts ["-Djline.internal.Log.trace=true"]
    :test-paths ["spec"]
    :repl-options {:init-ns user}
    :aot [reply.reader.jline.JlineInputReader]
    :main ^{:skip-aot true} reply.ReplyMain))
