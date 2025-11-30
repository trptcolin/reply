(ns reply.parsing-test
  (:require [clojure.test :as t]
            [reply.parsing :as p]))

(t/deftest parsed-forms
  (let [eof (Object.)
        read-line-fn (fn [state] (read-line))
        options {:request-exit eof :read-line-fn read-line-fn}]

    (t/testing "gets an eof when readLine says t/testing's done"
      (t/is (= [eof]
               (with-in-str ""
                 (doall (p/parsed-forms options))))))

    (t/testing "gets one form"
      (t/is (= ["foo"]
               (with-in-str "foo"
                 (doall (p/parsed-forms options))))))

    (t/testing "gets multiline forms"
      (t/is (= ["(+ 1 2\n3)"]
               (with-in-str "(+ 1 2\n3)"
                 (doall (p/parsed-forms options))))))

    (t/testing "gets multiline forms, with overlap"
      (t/is (= ["(+ 1 2\n3)" "(- 3\n1)"]
               (with-in-str "(+ 1 2\n3) (- 3\n1)"
                 (doall (p/parsed-forms options))))))

    (t/testing "gets multiple forms on a single line"
      (t/is (= ["1" "2" "3"]
               (with-in-str "1 2 3"
                 (doall (p/parsed-forms options))))))

    (t/testing "gets an empty couple of lines"
      (t/is (= [""]
               (with-in-str "\n\n"
                 (doall (p/parsed-forms options))))))

    (t/testing "gets wht/testingespace"
      (t/is (= [""]
               (with-in-str "  \n \n"
                 (doall (p/parsed-forms options))))))))

