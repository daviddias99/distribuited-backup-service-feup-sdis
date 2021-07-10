#!/bin/bash

killall rmiregistry
sleep 1
cd build
rmiregistry  &
sleep 1
cd ..
for i in $(seq 1 $1);
    do gnome-terminal --tab -- ./launch_peer.sh $i ap$i $2;
done