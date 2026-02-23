(ns reply.reader.jline.jline-input-reader-test
  (:require [clojure.test :as t])
  (:import [reply.reader.jline JlineInputReader]))

(defprotocol LineReader
  (readLine [_]))

(defrecord FakeJlineReader [f]
  LineReader
  (readLine [_] (f)))

(defn- fake-reader
  ([input-fn] (fake-reader input-fn #()))
  ([input-fn set-empty-prompt]
   (let [jline-reader (FakeJlineReader. input-fn)]
     (JlineInputReader. {:jline-reader jline-reader
                         :set-empty-prompt set-empty-prompt}))))

(t/deftest jline-input-reader
  (t/testing "reads a -1 when nothing is available"
    (t/is (= -1 (.read (fake-reader (constantly nil))))))

  (t/testing "reads a newline"
    (t/is (= 10 (.read (fake-reader (constantly ""))))))

  (t/testing "reads a few characters"
    (let [reader (fake-reader (constantly "abcd"))]
      (t/is (= [97 98 99 100 10]
               (for [_ (range 5)] (.read reader))))))

  (t/testing "calls for more input on a newline"
    (let [times-called (atom 0)
          reader (fake-reader (constantly "")
                              #(swap! times-called inc))]
      (t/is (= [10 10 10]
               (for [_ (range 3)] (.read reader))))
      (t/is (= 3 @times-called))))

  (t/testing "makes no more calls to readLine than are necessary"
    (let [times-called (atom 0)
          reader (fake-reader (constantly "ab")
                              #(swap! times-called inc))]
      (t/is (= [97 98 10 97 98 10 97 98 10 97]
               (for [_ (range 10)] (.read reader))))
      (t/is (= 4 @times-called)))))
