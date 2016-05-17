#!/bin/bash


PROJ=$PWD
TMP=/tmp/torrent
SERVER_FILE_INFOS=file_infos.properties
SERVER_PROPS=tracker.properties
CLIENT_PROPS=client.properties
REFERENCE_FILE=/home/sanya1/Videos/11

rm -rf $TMP
mkdir $TMP

mkdir $TMP/server
mkdir -p $TMP/client1/downloads
mkdir -p $TMP/client2/downloads
mkdir -p $TMP/client3/downloads

touch $TMP/server/$SERVER_FILE_INFOS
echo "file_infos_path=$TMP/server/$SERVER_FILE_INFOS" >> $TMP/server/$SERVER_PROPS

touch $TMP/client1/$CLIENT_PROPS
touch $TMP/client2/$CLIENT_PROPS
touch $TMP/client3/$CLIENT_PROPS

JAVA=$JAVA_HOME/bin/java

cd $PROJ
mvn assembly:assembly 

JAR=$PROJ/target/torrent-1.0-SNAPSHOT-jar-with-dependencies.jar
SERVER="$JAVA -cp $JAR ru.spbau.mit.TorrentTrackerMain"
CLIENT="$JAVA -cp $JAR ru.spbau.mit.TorrentClientMain"
FILE_SUFFIX=new_file

cd $TMP/server
$SERVER $SERVER_PROPS </dev/null &
PID0=$!

sleep 2


cd $TMP/client1

$CLIENT upload $REFERENCE_FILE
$CLIENT list
$CLIENT distribute 0 $REFERENCE_FILE $CLIENT_PROPS
$CLIENT run $CLIENT_PROPS 9000 </dev/null &
PID1=$!

sleep 2

cd $TMP/client2
$CLIENT download 0 $FILE_SUFFIX $CLIENT_PROPS
$CLIENT seeds 0
$CLIENT run $CLIENT_PROPS 10000 </dev/null &
PID2=$!

cd $TMP/client3
$CLIENT download 0 $FILE_SUFFIX $CLIENT_PROPS
$CLIENT seeds 0
$CLIENT run $CLIENT_PROPS 11000 </dev/null &
PID3=$!

while [[ $(ls $TMP/client2/downloads | grep ".part") != '' || $(ls $TMP/client3/downloads | grep ".part") != '' || ! -e $TMP/client3/downloads/$FILE_SUFFIX || ! -e $TMP/client2/downloads/$FILE_SUFFIX ]]; do
	sleep 2;
done


echo "STARTED CHECKING"

cmp --silent $TMP/client2/downloads/$FILE_SUFFIX $REFERENCE_FILE || echo "Bad file content for client 2"
cmp --silent $TMP/client3/downloads/$FILE_SUFFIX $REFERENCE_FILE || echo "Bad file content for client 3"

kill $PID0 $PID1 $PID2 $PID3
kill -9 $PID0 $PID1 $PID2 $PID3



