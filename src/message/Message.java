package message;

import javax.naming.directory.InvalidAttributeValueException;
import utils.Protocol;

public class Message extends SimpleMessage {
    private String fileID;
    private byte[] body;

    public Message(String version, MessageType messageType, String senderID, String fileID, byte[] body) throws InvalidAttributeValueException {     
        super(version, messageType, senderID);
        this.fileID = this.checkFileID(fileID);
        this.body = body;
    }

    @Override
    public String headerToString() {
        String header = this.getVersion() + " " + this.getMessageType().name()  + " " + this.getSenderID() + " " + this.getFileID() + " ";
        header += Protocol.CRLF + Protocol.CRLF;
        return header;
    }

    @Override
    public byte[] getBytes(){
        byte[] header = this.headerToString().getBytes();
        int bodyLength = 0;
        if(body != null)
            bodyLength = body.length;
        byte[] finalMessage = new byte[header.length + bodyLength];
        System.arraycopy(header, 0, finalMessage, 0, header.length);
        if(body != null)
            System.arraycopy(body, 0, finalMessage, header.length, bodyLength);

        return finalMessage;
    }

    public void setBody(byte[] body){
        this.body = body;
    }

    public String getFileID() {
        return fileID;
    }

    public byte[] getBody() {
        return body;
    }

    private String checkFileID(String fileID) throws InvalidAttributeValueException {
        if(!fileID.matches("[A-Fa-f\\d]{64}"))
            throw new InvalidAttributeValueException("Field ID must be hexadecimal with length 64 (32 bytes)");
        return fileID;        
    }
}