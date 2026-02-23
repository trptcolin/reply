(ns reply.parsing
  (:require [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rt]))

(defn subsequent-prompt-string [{:keys [prompt-string
                                        subsequent-prompt-string]}]
  (or subsequent-prompt-string
      (apply str (concat (repeat (- (count prompt-string)
                                    (count "#_=> "))
                                 \space)
                         "#_=> "))))

(defn- make-tracking-reader
  "Create a tools.reader-compatible reader over `text` that tracks
  its position via the `pos` atom. After each r/read call completes,
  @pos is the offset of the next unread character in `text`."
  [^String text pos]
  (let [len (count text)]
    (reify
      rt/Reader
      (read-char [_]
        (let [p @pos]
          (if (>= p len)
            nil
            (let [ch (.charAt text p)]
              (swap! pos inc)
              ch))))
      (peek-char [_]
        (let [p @pos]
          (if (>= p len)
            nil
            (.charAt text p))))
      rt/IPushbackReader
      (unread [_ ch]
        (when ch
          (swap! pos dec))))))

(defn- try-read
  "Attempt to read one form from `reader`.
  Returns {:status :ok :val v}
       or {:status :incomplete}
       or {:status :error}
       or {:status :eof}"
  [reader]
  (try
    (let [val (binding [r/*read-eval* false]
                (r/read {:eof ::eof :read-cond :allow :features #{:clj}}
                        reader))]
      (if (= ::eof val)
        {:status :eof}
        {:status :ok :val val}))
    (catch Exception e
      (let [eof-incomplete?
            (or (= :eof (:ex-kind (ex-data e)))
                (let [cause (.getCause e)]
                  (and (= :reader-exception (:type (ex-data e)))
                       cause
                       (= :eof (:ex-kind (ex-data cause))))))]
        (if eof-incomplete?
          {:status :incomplete}
          {:status :error})))))

(defn- read-forms
  "Read all complete forms from `text` using tools.reader.
  Returns [form-strings remaining-text] where remaining-text is the
  incomplete trailing portion (or nil if everything was consumed)."
  [text]
  (let [pos (atom 0)
        reader (make-tracking-reader text pos)]
    (loop [forms []]
      (let [start @pos
            result (try-read reader)]
        (case (:status result)
          :ok (let [end @pos
                    form-str (.trim (subs text start end))]
                (recur (conj forms form-str)))
          :eof [forms nil]
          :incomplete (let [remainder (.trim (subs text start))]
                        [forms remainder])
          :error (let [end @pos
                       form-str (.trim (subs text start end))]
                   (if (.isEmpty form-str)
                     [forms nil]
                     (recur (conj forms form-str)))))))))


(declare parsed-forms)

(defn- process-input [text-so-far next-text options]
  (let [text (if text-so-far
               (str text-so-far \newline next-text)
               next-text)
        [form-strings remainder] (read-forms text)]
    (if (and remainder (not (.isEmpty remainder)))
      (lazy-seq
        (concat form-strings
                (parsed-forms
                  (assoc options
                         :text-so-far remainder
                         :prompt-string
                         (subsequent-prompt-string options)))))
      (if (seq form-strings)
        form-strings
        (list "")))))

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
     (if (= :interrupted next-text)
       (list "")
       (process-input text-so-far next-text options))
     (list request-exit))))
