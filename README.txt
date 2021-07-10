# SDIS PROJECT 1

### Compiling

* To compile the solution the script 'compile.sh' should be executed while on the scripts folder. This script also deletes previous 'Peers' folder.
* Java version used:
    - openjdk version "11.0.6" 2020-01-14

### Versions

* Version 1.0: No enhancements;
* Version 1.1: Recover enhancement;
* Version 1.2: Recover and Backup enhancement;
* Version 1.3 and up: Recover, Backup and Delete enhancements;
  
### Running

* To run a single peer the script 'launch_peer.sh' should be used. This peers receives 2 or 3 arguments. If two arguments are given they are interpreted as the PeerID and the RMI access point (the version of the peer is set to 1.0). If three areguments are given in addition to the aforementioned ones, the peer version must also be given.
* The script launch_npeers.sh receives two arguments, the number of peers to launch and their version. The peers are launched in separate terminal windows. This script launches the rmiregistry in port 1099.
* The script kill_peers.sh kills all peers launched by the launch_peer or launch_npeers scripts.
* If the RMI registry is not launched by the user than the first peer to initiate will create the registry on port 1099.
* The commands.sh script is used to run commands in "batch mode"

### File strucutre

* The 'src' folder contains all the source code for the project
* The 'scripts' folder contains the scripts used for compiling and running
* The peers generate the following file structure:
* 
Peers
|
└───peerID
    │   backed_files
    │   deleted_files
    │   deleteChunkLedger
    |   storedChunkLedger
    |
    └───recovered_files
    |   │   file111.txt
    |   │   file112.pdf
    |   │   ...
    |
    └───fileID1
    |   │   chunkID1
    |   │   chunkID2
    |   │   ...    

### Authors

* up201705373- David Luís Dias da Silva
* up201704211- Manuel Monge dos Santos Pereira Coutinho  