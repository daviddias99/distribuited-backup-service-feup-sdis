#!/bin/bash

rm -r ./build
rm -r ./Peers
javac -cp ../src -d build ../src/client/PeerSetup.java
javac -cp ../src -d build ../src/client/TestApp.java