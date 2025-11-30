(ns reply.integration-test
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.repl]
            [reply.initialization :as initialization]
            [reply.main :as main]
            [reply.reader.simple-jline :as simple-jline]
            [nrepl.server :as server]
            [classlojure.core :as classlojure]))

(def ^:dynamic *server-port* nil)

;; TODO: this is easy but seems like wasted effort
;;       probably better to use pomegranate
(def nrepl
  {:local-path "spec/nrepl-0.8.3.jar"
   :remote-url "https://clojars.org/repo/nrepl/nrepl/0.8.3/nrepl-0.8.3.jar"})

(def clojure
  {:local-path "spec/clojure-1.7.0.jar"
   :remote-url "https://repo1.maven.org/maven2/org/clojure/clojure/1.7.0/clojure-1.7.0.jar"})

(defn ensure-test-jar [{:keys [local-path remote-url]}]
  (let [file (java.io.File. local-path)]
    (when-not (.exists file)
      (.createNewFile file)
      (let [out (java.io.FileOutputStream. file)
            in (io/input-stream remote-url)]
        (io/copy in out)))))

(ensure-test-jar clojure)
(ensure-test-jar nrepl)

(defmacro with-out-err [[out err] & body]
  `(let [~out (java.io.ByteArrayOutputStream.)
         ~err (java.io.ByteArrayOutputStream.)]
     (binding [*out* (java.io.PrintWriter. ~out)
               *err* (java.io.PrintWriter. ~err)]
       ~@body)))

(t/deftest standalone-mode
  (with-out-err [out err]
    (t/testing "prints help on startup and exits properly"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes "(* 21 2)\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (re-find #"42" (str out)))
      (t/is (re-find #"Commands: \(user/help\)" (str out)))
      (t/is (re-find #"Bye for now!\n" (str out)))))

  (with-out-err [out err]
    (t/testing "echoes a string back as a string"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes "\"test\"\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (re-find #"\"test\"" (str out)))))

  (with-out-err [out err]
    (t/testing "prints an error when given something that can't be read"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes ")\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (re-find #"Unmatched delimiter" (str out)))))

  (with-out-err [out err]
    (t/testing "puts read-time errors into *e"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes ")\n*e\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (re-find #"Unmatched delimiter" (str out)))
      (t/is (re-find #"user=> #error" (str out)))))

  (with-out-err [out err]
    (t/testing "does not print an error when given empty input lines"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes "\n\n\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (not (re-find #"EOF while reading" (str out))))
      (t/is (not (re-find #"RuntimeException" (str out))))))

  (with-out-err [out err]
    (t/testing "does not set *1 when given empty input lines"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes "424242\n\n\n(* 2 *1)\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (re-find #"848484" (str out)))))

  ;; this was broken even on speclj
  #_(with-out-err [out err]
    (t/testing "tab-completes clojure.core fns"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes "map\t\nexit\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (re-find #"mapcat" (str out)))
      (t/is (re-find #"map-indexed" (str out)))))

  #_(with-out-err [out err]
    (t/testing "tab-completes fns in aliased namespaces"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes
                                 "(require '[clojure.string :as s])\ns/\tsplt/testing\next/testing\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (re-find #"s/split" (str out)))
      (t/is (re-find #"s/replace" (str out)))))

  #_(with-out-err [out err]
    (t/testing "tab-completes without putting results into *1"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes "424242\nmap\t\b\b\b(* 2 *1)\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      (t/is (re-find #"848484" (str out)))))

  (with-out-err [out err]
    (t/testing "does not crash when printing throws an exception"
      (main/launch-standalone {:input-stream
                               (java.io.ByteArrayInputStream.
                                (.getBytes "(lazy-seq #())\n"))
                               :output-stream out})
      (t/is (= "" (str err)))
      ;; clean shutdown
      (t/is (re-find #"Bye for now" (str out))))))

(defn wrap-nrepl-server [f]
  (let [cl (classlojure/classlojure
            (str "file:" (:local-path nrepl))
            (str "file:" (:local-path clojure)))
        server-port (classlojure/eval-in
                     cl
                     '(do (require '[nrepl.server])
                          (import 'java.net.SocketException)
                          (def running-server (atom nil))
                          (let [server (nrepl.server/start-server)]
                            (reset! running-server server)
                            (:port server))))]
    (binding [*server-port* server-port]
      (try (f)
           (finally
             (classlojure/eval-in
              cl
              '(try (nrepl.server/stop-server @running-server)
                    (shutdown-agents)
                    (catch Throwable t
                      (.println System/err "nREPL shutdown failed")))))))))

(t/deftest nrepl-integration
  (with-out-err [out err]
    (t/testing "prints help on startup and exits properly"
      (wrap-nrepl-server (fn []
                           (main/launch-nrepl {:attach (str *server-port*)
                                               :input-stream
                                               (java.io.ByteArrayInputStream.
                                                (.getBytes "exit\n(println 'foobar)\n"))
                                               :output-stream out})))
      (t/is (= "" (str err)))
      (t/is (not (re-find #"foobar" (str out))))
      (t/is (re-find #"Bye for now!\n" (str out))))

    (t/testing "allows using doc"
      (wrap-nrepl-server (fn []
                           (main/launch-nrepl {:attach (str *server-port*)
                                               :input-stream
                                               (java.io.ByteArrayInputStream.
                                                (.getBytes "(doc map)\nexit\n"))
                                               :output-stream out})))
      (t/is (= "" (str err)))
      (t/is (re-find #"Returns a lazy sequence" (str out)))))

  ;; this was also broken on speclj
  #_(describe "completion"

    (t/testing "tab-completes clojure.core fns"
      (main/launch-nrepl {:attach (str *server-port*)
                          :input-stream
                          (java.io.ByteArrayInputStream.
                            (.getBytes "map\t\nexit\n"))
                          :output-stream out})
      (= "" (str err))
      (should-contain "mapcat" (str out))
      (should-contain "map-indexed" (str out)))

    (t/testing "tab-completes fns in aliased namespaces"
      (main/launch-nrepl {:attach (str *server-port*)
                          :input-stream
                          (java.io.ByteArrayInputStream.
                            (.getBytes "(require '[clojure.string :as s])\ns/\tsplit\nexit\n"))
                          :output-stream out})
      (= "" (str err))
      (should-contain "s/split" (str out))
      (should-contain "s/replace" (str out)))))
