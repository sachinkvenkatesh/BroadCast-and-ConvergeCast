#!/bin/bash


# Change this to your netid
netid=sxk159030

#
# Root directory of your project
PROJDIR=$HOME/CS6378/Project1

#
# This assumes your config file is named "config.txt"
# and is located in your project directory
#
CONFIG=$PROJDIR/configFile_2.txt
#
# Directory your java classes are in
#
BINDIR=$PROJDIR/bin

#
# Your main project class
#
PROG=SpanningTree

n=0
cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    echo $i
    read line
    echo $line
    while read line 
    do
    	# echo $line
        host=$( echo $line | awk '{ print $1 }' )

      	ssh $netid@$host java -cp $BINDIR $PROG $n $CONFIG > $PROJDIR/${n}.log &
	 #echo $n
        n=$(( n + 1 ))
    done
   
)


