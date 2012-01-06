java -cp lib/clojure-1.3.0.jar:lib/jline-2.9.1.jar:lib/jansi-1.4.jar:src/:classes/ clojure.main -m reply.core

# debugger
#java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -cp /home/colin/Projects/clojure/clojure-1.4.0-master-SNAPSHOT.jar:/home/colin/Projects/jline2/target/scala-2.9.0-1/jline_2.9.0-1-2.10.0-SNAPSHOT.jar:lib/jansi-1.4.jar:classes/:src/ clojure.main -m reply.core

# haxx
#java -cp lib/clojure-1.3.0.jar:/home/colin/Projects/jline2/target/scala-2.9.0-1/jline_2.9.0-1-2.10.0-SNAPSHOT.jar:lib/jansi-1.4.jar:src/:classes/ clojure.main -m reply.core
