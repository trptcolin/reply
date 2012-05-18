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

BASEDIR=$(dirname "$SCRIPT")

if [ -z "$USER_CP" ]; then
  USER_CP=""
fi

CP="$BASEDIR"/../src/clj/:\
"$BASEDIR"/../classes/
for j in "$BASEDIR"/../lib/*.jar; do
  CP=$CP:$j
done

java -Dfile.encoding=UTF-8 -cp "$USER_CP":"$CP" reply.ReplyMain "$@"

# for debugging:
# java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n \
#      -Djline.internal.Log.debug=true \
#      -Dfile.encoding=UTF-8 -cp $CP reply.ReplyMain "$@"

