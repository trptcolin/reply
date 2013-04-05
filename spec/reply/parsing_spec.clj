(ns reply.parsing-spec
  (:use [speclj.core]
        [reply.parsing]))

(describe "parsed-forms"
  (with eof (Object.))
  (with read-line-fn (fn [state] (read-line)))
  (with options {:request-exit @eof :read-line-fn @read-line-fn})

  (it "gets an eof when readLine says it's done"
    (should= [@eof]
             (with-in-str ""
               (doall (parsed-forms @options)))))

  (it "gets one form"
    (should= ["foo"]
             (with-in-str "foo"
               (doall (parsed-forms @options)))))

  (it "gets multiline forms"
    (should= ["(+ 1 2\n3)"]
             (with-in-str "(+ 1 2\n3)"
               (doall (parsed-forms @options)))))

  (it "gets multiline forms, with overlap"
    (should= ["(+ 1 2\n3)" "(- 3\n1)"]
             (with-in-str "(+ 1 2\n3) (- 3\n1)"
               (doall (parsed-forms @options)))))

  (it "gets multiple forms on a single line"
    (should= ["1" "2" "3"]
             (with-in-str "1 2 3"
               (doall (parsed-forms @options)))))

  (it "gets an empty couple of lines"
    (should= [""]
             (with-in-str "\n\n"
               (doall (parsed-forms @options)))))

  (it "gets whitespace"
    (should= [""]
             (with-in-str "  \n \n"
               (doall (parsed-forms @options))))))

