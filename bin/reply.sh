#!/bin/sh


# normalize $0 on certain BSDs
if [ "$(dirname "$0")" = "." ]; then
    SCRIPT="$(which $(basename "$0"))"
else
    SCRIPT="$0"
fi

# resolve symlinks to the script itself portably
while [ -h "$SCRIPT" ] ; do
    ls=`ls -ld "$SCRIPT"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        SCRIPT="$link"
    else
        SCRIPT="$(dirname "$SCRIPT"$)/$link"
    fi
done

BASEDIR=$(dirname $SCRIPT)

CP=$BASEDIR/../src/:\
$BASEDIR/../classes/
for j in $BASEDIR/../lib/*.jar; do
  CP=$CP:$j
done
java -cp $CP clojure.main -m reply.main

# debugger
#java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -cp /home/colin/Projects/clojure/clojure-1.4.0-master-SNAPSHOT.jar:/home/colin/Projects/jline2/target/scala-2.9.0-1/jline_2.9.0-1-2.10.0-SNAPSHOT.jar:lib/jansi-1.4.jar:classes/:src/ clojure.main -m reply.main

# haxx
#java -cp lib/clojure-1.3.0.jar:/home/colin/Projects/jline2/target/scala-2.9.0-1/jline_2.9.0-1-2.10.0-SNAPSHOT.jar:lib/jansi-1.4.jar:src/:classes/ clojure.main -m reply.main
