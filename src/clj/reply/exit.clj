(ns reply.exit)

(defn done-commands [eof]
  #{eof 'quit 'exit '(quit) '(exit)
    "quit" "(quit)" "exit" "(exit)"})

(defn done? [eof expression]
  ((done-commands eof) expression))

