#!/bin/bash

if [ $# -eq 2 ]
then
    java -cp build client.PeerSetup 1.0 $1 $2 225.0.0.1 25564 225.0.0.1 25565 225.0.0.1 25566
fi

if [ $# -eq 3 ]
then
    java -cp build client.PeerSetup $3 $1 $2 225.0.0.1 25564 225.0.0.1 25565 225.0.0.1 25566
fi