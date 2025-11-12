(ns reply.parsing
  (:require [net.cgrand.sjacket :as sjacket]
            [net.cgrand.sjacket.parser :as sjacket.parser]))

(defn node-completed? [node]
  (or (not= :net.cgrand.parsley/unfinished (:tag node))
      (some #(= :net.cgrand.parsley/unexpected (:tag %))
            (tree-seq :tag :content node))))

(defn subsequent-prompt-string [{:keys [prompt-string
                                        subsequent-prompt-string]}]
  (or subsequent-prompt-string
      (apply str (concat (repeat (- (count prompt-string)
                                    (count "#_=> "))
                                 \space)
                         "#_=> "))))

(defn remove-whitespace [forms]
  (remove #(contains? #{:whitespace :comment :discard} (:tag %))
          forms))

(defn reparse [text-so-far next-text]
  (sjacket.parser/parser
    (if text-so-far
      (str text-so-far \newline next-text)
      next-text)))

(declare parsed-forms)

(defn process-parse-tree [parse-tree options]
  (let [complete-forms (take-while node-completed? (:content parse-tree))
        remainder (drop-while node-completed? (:content parse-tree))
        form-strings (map sjacket/str-pt
                          (remove-whitespace complete-forms))]
    (cond
      (seq remainder)
        (lazy-seq
          (concat form-strings
                  (parsed-forms
                    (assoc options
                           :text-so-far
                           (apply str (map sjacket/str-pt remainder))
                           :prompt-string
                           (subsequent-prompt-string options)))))
      (seq form-strings)
        form-strings
      :else
        (list ""))))

(defn parsed-forms
  "Requires the following options:
  - request-exit: the value to return on completion/EOF
  - read-line-fn: a function that takes an options map that will include :ns
                and :prompt-string.
  - ns: the current ns, available because it can be useful for read-line-fn
  - prompt-string: for customizing the prompt
  - text-so-far: mostly useful in the recursion
  And returns a seq of *strings* representing complete forms."
  ([options]
   (parsed-forms ((:read-line-fn options) options) options))
  ([next-text {:keys [request-exit text-so-far] :as options}]
   (if next-text
     (let [interrupted? (= :interrupted next-text)
           parse-tree (when-not interrupted? (reparse text-so-far next-text))]
       (if (or interrupted? (empty? (:content parse-tree)))
         (list "")
         (process-parse-tree parse-tree options)))
     (list request-exit))))
