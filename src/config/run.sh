#!/bin/sh
export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8787"
java $JAVA_OPTS -jar eve.jar ./config.properties