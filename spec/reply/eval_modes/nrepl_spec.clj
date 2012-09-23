(ns reply.eval-modes.nrepl-spec
  (:use [speclj.core]
        [reply.eval-modes.nrepl]))

(describe "parsed-forms"
  (with eof (Object.))

  (it "gets an eof when readLine says it's done"
    (should= [@eof]
             (with-in-str ""
               (doall (parsed-forms @eof)))))

  (it "gets one form"
    (should= ["foo"]
             (with-in-str "foo"
               (doall (parsed-forms @eof)))))

  (it "gets multiline forms"
    (should= ["(+ 1 2\n3)"]
             (with-in-str "(+ 1 2\n3)"
               (doall (parsed-forms @eof)))))

  (it "gets multiline forms, with overlap"
    (should= ["(+ 1 2\n3)" "(- 3\n1)"]
             (with-in-str "(+ 1 2\n3) (- 3\n1)"
               (doall (parsed-forms @eof)))))

  (it "gets multiple forms on a single line"
    (should= ["1" "2" "3"]
             (with-in-str "1 2 3"
               (doall (parsed-forms @eof)))))

  (it "gets an empty couple of lines"
    (should= [""]
             (with-in-str "\n\n"
               (doall (parsed-forms @eof)))))

  (it "gets whitespace"
    (should= [""]
             (with-in-str "  \n \n"
               (doall (parsed-forms @eof))))))
