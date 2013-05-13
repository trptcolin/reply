(ns reply.integration-spec
  (:require [speclj.core :refer :all]
            [reply.exit :as exit]
            [reply.initialization :as initialization]
            [reply.main :as main]
            [reply.reader.simple-jline :as simple-jline]
            [clojure.tools.nrepl.server :as server]
            [classlojure.core :as classlojure]
            [clojure.java.io :as io]
            [clojure.repl]))

(def ^:dynamic *server-port* nil)

;; TODO: this is easy but seems wasteful; probably better to use pomegranate
(def nrepl-023
  {:local-path "spec/nrepl-0.2.3.jar"
   :remote-url "http://repo1.maven.org/maven2/org/clojure/tools.nrepl/0.2.3/tools.nrepl-0.2.3.jar"})

(def clojure-151
  {:local-path "spec/clojure-1.5.1.jar"
   :remote-url "http://repo1.maven.org/maven2/org/clojure/clojure/1.5.1/clojure-1.5.1.jar"})

(defn ensure-test-jar [{:keys [local-path remote-url]}]
  (let [file (java.io.File. local-path)]
    (when-not (.exists file)
      (.createNewFile file)
      (let [out (java.io.FileOutputStream. file)
            in (io/input-stream remote-url)]
        (io/copy in out)))))

(describe "nrepl integration" (tags :slow)

  (around [f]
    (ensure-test-jar clojure-151)
    (ensure-test-jar nrepl-023)
    (let [cl (classlojure/classlojure
               (str "file:" (:local-path nrepl-023))
               (str "file:" (:local-path clojure-151)))
          server-port
          (classlojure/eval-in
            cl
            '(do (require '[clojure.tools.nrepl.server :as server])
                 (:port (server/start-server))))]
      (binding [*server-port* server-port]
        (f))))

  (describe "initialization"

    (it "prints help on startup and exits properly"
      (let [fake-out (java.io.ByteArrayOutputStream.)]
        (binding [*out* (java.io.PrintWriter. fake-out)]
          (with-redefs [exit/exit #()]
            (main/launch-nrepl {:attach (str *server-port*)
                                :input-stream
                                (java.io.ByteArrayInputStream.
                                  (.getBytes "exit\n(println 'foobar)\n"))
                                :output-stream fake-out}))
          (should-contain (with-out-str (initialization/help)) (str fake-out))
          (should-not-contain "foobar" (str fake-out)))))

    (it "allows using doc"
      (let [fake-out (java.io.ByteArrayOutputStream.)]
        (binding [*out* (java.io.PrintWriter. fake-out)]
          (with-redefs [exit/exit #()]
            (main/launch-nrepl {:attach (str *server-port*)
                                :input-stream
                                (java.io.ByteArrayInputStream.
                                  (.getBytes "(doc map)\n"))
                                :output-stream fake-out}))
          (should-contain (with-out-str (clojure.repl/doc map))
                          (str fake-out)))))
    )

  (describe "completion"

    (it "tab-completes clojure.core fns"
      (let [fake-out (java.io.ByteArrayOutputStream.)]
        (binding [*out* (java.io.PrintWriter. fake-out)]
          (with-redefs [exit/exit #()]
            (main/launch-nrepl {:attach (str *server-port*)
                                :input-stream
                                (java.io.ByteArrayInputStream.
                                  (.getBytes "(map\t"))
                                :output-stream fake-out}))
          (should-contain "mapcat" (str fake-out))
          (should-contain "map-indexed" (str fake-out))))

      )
    )
  )
