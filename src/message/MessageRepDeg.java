package message;

import javax.naming.directory.InvalidAttributeValueException;

import utils.Protocol;

public class MessageRepDeg extends MessageChunkNo {
    private int replicationDeg;

    public MessageRepDeg(String version, MessageType messageType, String senderID, String fileID, String chunkNo, String replicationDeg, byte[] message) throws InvalidAttributeValueException {     
        super(version, messageType, senderID, fileID, chunkNo, message);
       
        this.replicationDeg = this.checkRepDeg(replicationDeg);

    }

    @Override
    public String headerToString() {
        String header = this.getVersion() + " " + this.getMessageType().name()  + " " + this.getSenderID() + " " + this.getFileID() +  " " + this.getChunkNo() + " " + this.getReplicationDegree() + " ";
        header += Protocol.CRLF + Protocol.CRLF;
        return header;
    }

    public int getReplicationDegree() {
        return replicationDeg;
    }

    private int checkRepDeg(String replicationDeg) throws InvalidAttributeValueException{
        if(!replicationDeg.matches("\\d"))
            throw new InvalidAttributeValueException("Replication Degree must be a number lower than 10");
        return Integer.parseInt(replicationDeg);
    }
}