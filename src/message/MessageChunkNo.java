package message;

import javax.naming.directory.InvalidAttributeValueException;

import utils.Protocol;


public class MessageChunkNo extends Message {
    private int chunkNo;

    public MessageChunkNo(String version, MessageType messageType, String senderID, String fileID, String chunkNo, byte[] body) throws InvalidAttributeValueException {
        super(version, messageType, senderID, fileID, body);
        this.chunkNo = this.checkChunkNo(chunkNo);
    }

	@Override
    public String headerToString() {
        String header = this.getVersion() + " " + this.getMessageType().name()  + " " + this.getSenderID() + " " + this.getFileID() +  " " + this.getChunkNo() + " ";
        header += Protocol.CRLF + Protocol.CRLF;
        return header;
    }

    public int getChunkNo() {
        return this.chunkNo;
    }

    private int checkChunkNo(String chunkNo) throws InvalidAttributeValueException{
        if(!chunkNo.matches("[\\d]{1,6}"))
            throw new InvalidAttributeValueException("Chunk Number must be a number lower than 1.000.000");
        return Integer.parseInt(chunkNo);
    }
}