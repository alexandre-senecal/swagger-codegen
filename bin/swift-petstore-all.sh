#!/bin/sh

SCRIPT="$0"

while [ -h "$SCRIPT" ] ; do
  ls=`ls -ld "$SCRIPT"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT=`dirname "$SCRIPT"`/"$link"
  fi
done

if [ ! -d "${APP_DIR}" ]; then
  APP_DIR=`dirname "$SCRIPT"`/..
  APP_DIR=`cd "${APP_DIR}"; pwd`
fi

executable="./modules/swagger-codegen-cli/target/swagger-codegen-cli.jar"

if [ ! -f "$executable" ]
then
  mvn clean package
fi

# if you've executed sbt assembly previously it will use that instead.
export JAVA_OPTS="${JAVA_OPTS} -XX:MaxPermSize=256M -Xmx1024M -DloggerPath=conf/log4j.properties"
ags="$@ generate -t modules/swagger-codegen/src/main/resources/swift -i modules/swagger-codegen/src/test/resources/2_0/petstore.json -l swift -c ./bin/swift-petstore.json -o samples/client/petstore/swift/default"

echo "#### Petstore Swift API client (default) ####"
# java $JAVA_OPTS -jar $executable $ags

ags="$@ generate -t modules/swagger-codegen/src/main/resources/swift -i modules/swagger-codegen/src/test/resources/2_0/petstore.json -l swift -c ./bin/swift-petstore-promisekit.json -o samples/client/petstore/swift/promisekit"
echo "#### Petstore Swift API client (promisekit) ####"
# java $JAVA_OPTS -jar $executable $ags

ags="$@ generate -t modules/swagger-codegen/src/main/resources/swift -i modules/swagger-codegen/src/test/resources/2_0/petstore.json -l swift -c ./bin/swift-petstore-rxswift.json -o samples/client/petstore/swift/rxswift"
echo "#### Petstore Swift API client (rxswift) ####"
# java $JAVA_OPTS -jar $executable $ags
