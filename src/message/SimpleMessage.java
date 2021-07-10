package message;

import utils.Protocol;
import javax.naming.directory.InvalidAttributeValueException;


public class SimpleMessage {
    private String version;
    private MessageType messageType;
    private String senderID;

    public SimpleMessage(String version, MessageType messageType, String senderID)
            throws InvalidAttributeValueException {
        this.version = this.checkVersion(version);
        this.messageType = messageType;
        this.senderID = senderID;

    }

    public String headerToString() {
        String header = this.getVersion() + " " + this.getMessageType().name()  + " " + this.getSenderID() + " ";
        header += Protocol.CRLF + Protocol.CRLF;
        return header;
    }

    public byte[] getBytes(){
        return this.headerToString().getBytes();
    }

    public String getVersion() {
        return version;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getSenderID() {
        return senderID;
    }

    private String checkVersion(String version) throws InvalidAttributeValueException {
        if(version.length() != 3)
            throw new InvalidAttributeValueException("Version length must be equal to 3: <n>.<m>");
        if(version.charAt(1) != '.')
            throw new InvalidAttributeValueException("Version format must be <n>.<m>");
        return version;
    }


}