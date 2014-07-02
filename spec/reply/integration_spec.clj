(ns reply.integration-spec
  (:require [speclj.core :refer :all]
            [reply.initialization :as initialization]
            [reply.main :as main]
            [reply.reader.simple-jline :as simple-jline]
            [clojure.tools.nrepl.server :as server]
            [classlojure.core :as classlojure]
            [clojure.java.io :as io]
            [clojure.repl]))

(def ^:dynamic *server-port* nil)

(defmacro with-fake-printing [fake-out fake-err & body]
  `(binding [*out* (java.io.PrintWriter. ~fake-out)
             *err* (java.io.PrintWriter. ~fake-err)]
     ~@body))

;; TODO: this is easy but seems like wasted effort
;;       probably better to use pomegranate
(def nrepl
  {:local-path "spec/nrepl-0.2.3.jar"
   :remote-url "http://repo1.maven.org/maven2/org/clojure/tools.nrepl/0.2.3/tools.nrepl-0.2.3.jar"})

(def clojure
  {:local-path "spec/clojure-1.5.1.jar"
   :remote-url "http://repo1.maven.org/maven2/org/clojure/clojure/1.5.1/clojure-1.5.1.jar"})
  ;{:local-path "spec/clojure-1.6.0-master-SNAPSHOT.jar"
  ; :remote-url "file:///Users/colin/.m2/repository/org/clojure/clojure/1.6.0-master-SNAPSHOT/clojure-1.6.0-master-SNAPSHOT.jar"})

(defn ensure-test-jar [{:keys [local-path remote-url]}]
  (let [file (java.io.File. local-path)]
    (when-not (.exists file)
      (.createNewFile file)
      (let [out (java.io.FileOutputStream. file)
            in (io/input-stream remote-url)]
        (io/copy in out)))))

(ensure-test-jar clojure)
(ensure-test-jar nrepl)

(describe "standalone mode"
  (with fake-out (java.io.ByteArrayOutputStream.))
  (with fake-err (java.io.ByteArrayOutputStream.))

  (around [it]
    (binding [*out* (java.io.PrintWriter. @fake-out)
              *err* (java.io.PrintWriter. @fake-err)]
      (it)))


  (it "prints help on startup and exits properly"
    (main/launch-standalone {:input-stream
                             (java.io.ByteArrayInputStream.
                               (.getBytes "(* 21 2)\n"))
                             :output-stream @fake-out})
    (should-contain "42" (str @fake-out))
    (should-contain (with-out-str (initialization/help)) (str @fake-out))
    (should-contain "Bye for now!\n" (str @fake-out)))

  (it "echoes a string back as a string"
    (main/launch-standalone {:input-stream
                             (java.io.ByteArrayInputStream.
                               (.getBytes "\"test\"\n"))
                             :output-stream @fake-out})
    (should-contain "\"test\"" (str @fake-out)))

  (it "prints an error when given something that can't be read"
    (main/launch-standalone {:input-stream
                             (java.io.ByteArrayInputStream.
                               (.getBytes ")\n"))
                             :output-stream @fake-out})
    (should-contain "Unmatched delimiter" (str @fake-out)))

  (it "puts read-time errors into *e"
    (main/launch-standalone {:input-stream
                             (java.io.ByteArrayInputStream.
                               (.getBytes ")\n*e\n"))
                             :output-stream @fake-out})
    (should-contain #"Unmatched delimiter.+\n.+\n.+Unmatched delimiter" (str @fake-out)))

  (it "does not print an error when given empty input lines"
    (main/launch-standalone {:input-stream
                             (java.io.ByteArrayInputStream.
                               (.getBytes "\n\n\n"))
                             :output-stream @fake-out})
    (should-not-contain "EOF while reading" (str @fake-out))
    (should-not-contain "RuntimeException" (str @fake-out)))

  (it "does not set *1 when given empty input lines"
    (main/launch-standalone {:input-stream
                             (java.io.ByteArrayInputStream.
                               (.getBytes "424242\n\n\n(* 2 *1)\n"))
                             :output-stream @fake-out})
    (should-contain "848484" (str @fake-out)))

  (it "tab-completes clojure.core fns"
    (main/launch-standalone {:input-stream
                             (java.io.ByteArrayInputStream.
                               (.getBytes "map\t\nexit\n"))
                             :output-stream @fake-out})
    (should-contain "mapcat" (str @fake-out))
    (should-contain "map-indexed" (str @fake-out)))

  (it "tab-completes without putting results into *1"
    (main/launch-standalone {:input-stream
                             (java.io.ByteArrayInputStream.
                               (.getBytes "424242\nmap\t\b\b\b(* 2 *1)\n"))
                             :output-stream @fake-out})
    (should-contain "848484" (str @fake-out)))
  )

(describe "nrepl integration" (tags :slow)

  (around [f]
    (let [cl (classlojure/classlojure
               (str "file:" (:local-path nrepl))
               (str "file:" (:local-path clojure)))
          server-port
          (classlojure/eval-in
            cl
            '(do (require '[clojure.tools.nrepl.server])
                 (import 'java.net.SocketException)
                 (def running-server (atom nil))
                 (let [server (clojure.tools.nrepl.server/start-server)]
                   (reset! running-server server)
                   (:port server))))]
      (binding [*server-port* server-port]
        (try (f)
          (finally
            (classlojure/eval-in
              cl
              '(try (clojure.tools.nrepl.server/stop-server @running-server)
                 (shutdown-agents)
                 (catch Throwable t
                   (.println System/err "nREPL shutdown failed")))))))))

  (with fake-out (java.io.ByteArrayOutputStream.))
  (with fake-err (java.io.ByteArrayOutputStream.))

  (around [it]
    (with-redefs [shutdown-agents #()]
      (binding [*out* (java.io.PrintWriter. @fake-out)
                *err* (java.io.PrintWriter. @fake-err)]
        (it))))

  (describe "initialization"

    (it "prints help on startup and exits properly"
      (main/launch-nrepl {:attach (str *server-port*)
                          :input-stream
                          (java.io.ByteArrayInputStream.
                            (.getBytes "exit\n(println 'foobar)\n"))
                          :output-stream @fake-out})
      (should-contain (with-out-str (initialization/help)) (str @fake-out))
      (should-not-contain "foobar" (str @fake-out))
      (should-contain "Bye for now!\n" (str @fake-out)))

    (it "allows using doc"
      (main/launch-nrepl {:attach (str *server-port*)
                          :input-stream
                          (java.io.ByteArrayInputStream.
                            (.getBytes "(doc map)\nexit\n"))
                          :output-stream @fake-out})
      (should-contain (with-out-str (clojure.repl/doc map))
                      (str @fake-out))))

  (describe "completion"

    (it "tab-completes clojure.core fns"
      (main/launch-nrepl {:attach (str *server-port*)
                          :input-stream
                          (java.io.ByteArrayInputStream.
                            (.getBytes "map\t\nexit\n"))
                          :output-stream @fake-out})
      (should-contain "mapcat" (str @fake-out))
      (should-contain "map-indexed" (str @fake-out)))))
