#!/bin/bash
if [ -z "$JAVA6" ]; then
  echo "Set env var JAVA6" &&
  exit 1
fi
if [ -z "$JAVAC6" ]; then
  echo "Set env var JAVAC6" &&
  exit 1
fi
cd cfr &&
mvn -Dmaven.compiler.fork=true -Dmaven.compiler.executable=$JAVAC6 -DjavadocExecutable=/usr/bin/javadoc clean package verify install &&
cd .. &&
cd javacompilelib &&
mvn -Dmaven.compiler.fork=true -Dmaven.compiler.executable=$JAVAC6 -Djvm=$JAVA6 clean package verify install &&
cd .. &&
cd fabricmerge &&
mvn clean package verify install &&
cd .. &&
cd brachyura &&
mvn clean package verify &&
cd .. &&
cd bootstrap &&
mvn clean package verify &&
cd .. &&
cd build &&
mvn clean package verify &&
java -jar ./target/brachyura-build-0.jar
cd ..