count=`/usr/local/jdk1.8.0_45/bin/jps -l | grep ETL | wc -l`
if [ $count -lt 16 ]
then
    ((c=16-$count))
    for((i=0;i<$c;i++))
    do
        nohup /usr/local/kafkastream/bin/etl-kafkastream.sh configFile=conf/GZDPI.xml &
        echo "start $i  stream agent..."
    done
fi
