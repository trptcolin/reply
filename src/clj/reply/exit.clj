(ns reply.exit)

(defn done-commands [eof]
  #{eof 'quit 'exit '(quit) '(exit)
    "quit" "(quit)" "exit" "(exit)"})

(defn done? [eof expression]
  ((done-commands eof) expression))

(defn ^:dynamic exit
  "Exits the REPL. Warning: this does (shutdown-agents). Rebind if you don't
  want that."
  []
  (shutdown-agents)
  (print "Bye for now!")
  (flush))

