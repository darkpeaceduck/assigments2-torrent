#!/bin/bash


PROJ=$PWD
TMP=/tmp/torrent
SERVER_FILE_INFOS=file_infos.properties
SERVER_PROPS=tracker.properties

rm -rf $TMP
mkdir $TMP

mkdir $TMP/server

touch $TMP/server/$SERVER_FILE_INFOS
echo "file_infos_path=$TMP/server/$SERVER_FILE_INFOS" >> $TMP/server/$SERVER_PROPS


JAVA=$JAVA_HOME/bin/java

cd $PROJ
mvn assembly:assembly 

JAR=$PROJ/target/torrent-1.0-SNAPSHOT-jar-with-dependencies.jar
SERVER="$JAVA -cp $JAR ru.spbau.mit.TorrentTrackerMain"

cd $TMP/server
$SERVER $SERVER_PROPS </dev/null


