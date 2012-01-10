(ns reply.reader.jline.jline-input-reader-test
  (:use [midje.sweet])
  (:import [reply.reader.jline JlineInputReader]))

(defprotocol LineReader
  (readLine [_]))

(defrecord FakeJlineReader [f]
  LineReader
  (readLine [this] (f)))

(defn fake-reader
  ([input-fn] (fake-reader input-fn #()))
  ([input-fn set-empty-prompt]
    (let [jline-reader (FakeJlineReader. input-fn)]
      (JlineInputReader. {:jline-reader jline-reader
                          :set-empty-prompt set-empty-prompt}))))

(fact "reading nothing gives nothing"
  (.read (fake-reader (constantly nil))) => -1)

(fact "reading an empty line gives a newline"
  (.read (fake-reader (constantly ""))) => 10)

(let [reader (fake-reader (constantly "abcd"))]
  (fact "reading a few characters works"
    (for [i (range 5)]
      (.read reader)) => [97 98 99 100 10]))

(let [times-readLine-called (atom 0)
      reader (fake-reader (constantly "") #(swap! times-readLine-called inc))]
  (facts "it calls for more input on a newline"
    (for [i (range 3)]
      (.read reader)) => [10 10 10]
    @times-readLine-called => 3))

(let [times-readLine-called (atom 0)
      reader (fake-reader (constantly "ab") #(swap! times-readLine-called inc))]
  (facts "no more calls to readLine than are necessary"
    (for [i (range 10)]
      (.read reader)) => [97 98 10 97 98 10 97 98 10 97]
    @times-readLine-called => 4))

