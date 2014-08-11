#!/bin/sh
mvn clean package install
chmod -R 777 dist
export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8787 -cp dist/lib/json-simple-1.1.jar"
java $JAVA_OPTS -jar dist/eve.jar config.properties dist/resources/directives.json