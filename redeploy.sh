#!/bin/bash

mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dgpg.skip=true

RED5_DIR=$TMPDIR/ant-media-server
RED5_DIR=~/softwares/ant-media-server

RED5_LIB_DIR=$RED5_DIR/lib

RED5_JAR=./target/red5-server-common.jar

SRC_CONF_DIR=../Ant-Media-Server/src/main/server/conf/

#remove older version
rm $RED5_LIB_DIR/red5-server-common*

#copy red5 jar from target dir to red5 dir
cp  $RED5_JAR  $RED5_LIB_DIR/

cp -rf $SRC_CONF_DIR   $RED5_DIR/conf

ANT_MEDIA_2=~/softwares/ant-media-server-2

cp $RED5_JAR $ANT_MEDIA_2/lib



#go to red5 dir
cd $RED5_DIR

#shutdown red5
#./red5-shutdown.sh


#start red5
#./red5.sh
