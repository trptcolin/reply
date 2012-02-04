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

SKIP_CD=false
if [[ $@ =~ "--no-clojuredocs" ]]
then
	SKIP_CD=true
fi

CD_DEPS=( cd-client
	clj-http
	httpclient
	httpcore
	commons-logging
	commons-codec
	commons-io
	cheshire
	jackson-core-asl
	jackson-smile 
)

function is_cd_dep() {
	local dep=`basename $1`
	for d in ${CD_DEPS[@]}
	do
		if expr ${dep} : ${d} > /dev/null
		then
			return 0
		fi
	done
	return 1
}

CP="$BASEDIR"/../src/clj/:\
"$BASEDIR"/../classes/
for j in "$BASEDIR"/../lib/*.jar; do
	if ! ($SKIP_CD && is_cd_dep $j)
	then
		CP=$CP:$j
	fi
done

java -cp "$CP":"$USER_CP" reply.ReplyMain "$@"

# For jline debugging:
# java -Djline.internal.Log.debug=true -cp $CP reply.ReplyMain "$@"

