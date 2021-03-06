#!/bin/sh
##run etl project main class ETLRunner



  BASEDIR=`dirname "$0"`/..
  cd $BASEDIR
  
  BIGDATA_CLASSPATH="$BASEDIR/conf:$BASEDIR/lib/"
  for i in "$BASEDIR"/lib/*.jar
  do
    BIGDATA_CLASSPATH="$BIGDATA_CLASSPATH:$i"
  done


  #RUN_CMD="\"$JAVA_HOME/bin/java\""
  RUN_CMD="java "
  RUN_CMD="$RUN_CMD -classpath \"$BIGDATA_CLASSPATH\""
  RUN_CMD="$RUN_CMD -Xmx8G -Xms8G "
  RUN_CMD="$RUN_CMD com.gsta.bigdata.etl.ETLRunner $@"
  
  echo $RUN_CMD
  eval $RUN_CMD


