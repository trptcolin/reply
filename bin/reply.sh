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

