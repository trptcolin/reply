(ns reply.reader.jline.jline-input-reader-spec
  (:use [speclj.core])
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

(describe "JLineInputReader"
  (it "reads a -1 when nothing is available"
    (should= -1 (.read (fake-reader (constantly nil)))))

  (it "reads a newline"
    (should= 10 (.read (fake-reader (constantly "")))))

  (it "reads a few characters"
    (let [reader (fake-reader (constantly "abcd"))]
      (should= [97 98 99 100 10]
               (for [i (range 5)] (.read reader)))))

  (it "calls for more input on a newline"
    (let [times-readLine-called (atom 0)
          reader (fake-reader (constantly "")
                              #(swap! times-readLine-called inc))]
      (should= [10 10 10]
               (for [i (range 3)] (.read reader)))
      (should= 3 @times-readLine-called)))

  (it "makes no more calls to readLine than are necessary"
    (let [times-readLine-called (atom 0)
          reader (fake-reader (constantly "ab")
                              #(swap! times-readLine-called inc))]
      (should= [97 98 10 97 98 10 97 98 10 97]
               (for [i (range 10)] (.read reader)))
      (should= 4 @times-readLine-called))))
