#!/bin/sh
mvn install:install-file -DgroupId=org.jsr-305 -DartifactId=ri -Dversion=v0r47 -Dpackaging=jar -Dfile=../../third_party/runtime/jsr305/jsr305.jar
mvn install:install-file -DgroupId=com.google.guava -DartifactId=guava -Dversion=r07 -Dpackaging=jar -Dclassifier=gwt -Dfile=../../third_party/runtime/guava/guava-r07-gwt.jar
mvn install:install-file -DgroupId=com.glines.socketio -DartifactId=socketio -Dversion=20101204 -Dpackaging=jar -Dclassifier=sources -Dfile=../../third_party/runtime/socketio/socketio-java-src.jar
mvn install:install-file -DgroupId=com.sixfire.websocket -DartifactId=websocket -Dversion=9e791658f667ed2e3bea7dfa84a4b55ea39aba55 -Dpackaging=jar -Dfile=../../third_party/runtime/websocket/websocket.jar
