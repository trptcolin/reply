(ns reply.exit)

(defn done-commands [eof]
  #{eof 'quit 'exit '(quit) '(exit)
    "quit" "(quit)" "exit" "(exit)"})

(defn done? [eof expression]
  ((done-commands eof) expression))

(defn exit
  "Exits the REPL. This is fairly brutal, does (System/exit 0)."
  []
  (shutdown-agents)
  (print "Bye for now!")
  (flush)
  (System/exit 0))

