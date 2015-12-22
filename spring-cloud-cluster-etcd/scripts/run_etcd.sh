#!/bin/bash


# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
	ls=`ls -ld "$PRG"`
	link=`expr "$ls" : '.*-> \(.*\)$'`
	if expr "$link" : '/.*' > /dev/null; then
		PRG="$link"
	else
		PRG=`dirname "$PRG"`"/$link"
	fi
done
cd "`dirname \"$PRG\"`/../"

docker-compose up -d
