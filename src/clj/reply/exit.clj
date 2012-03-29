(ns reply.exit)

(defn done-commands [eof]
  #{eof 'quit 'exit '(quit) '(exit)})

(defn done? [eof expression]
  ((done-commands eof) expression))

(defn exit
  "Exits the REPL. This is fairly brutal, does (System/exit 0)."
  []
  (shutdown-agents)
  (println "Bye for now!")
  (System/exit 0))

