package message;

import javax.naming.directory.InvalidAttributeValueException;

import client.Peer;
import utils.Protocol;

public class MessageFactory {

    public static MessageChunkNo createStored(String fileID, int chunkNo) throws InvalidAttributeValueException {
        return new MessageChunkNo(Protocol.VERSION, MessageType.STORED, Peer.getID() + "", fileID, chunkNo + "", null);
    }

    public static MessageRepDeg createPutChunk(String fileID, int chunkNo, int replicationDegree, byte[] body)
            throws InvalidAttributeValueException {
        return new MessageRepDeg(Protocol.VERSION, MessageType.PUTCHUNK, Peer.getID() + "", fileID, chunkNo + "", replicationDegree + "", body);
    }

    public static MessageChunkNo createGetChunk(String fileID, int chunkNo)
            throws InvalidAttributeValueException {
        return new MessageChunkNo(Protocol.VERSION, MessageType.GETCHUNK, Peer.getID() + "", fileID, chunkNo + "", null);
    }

    public static MessageChunkNo createChunk(String fileID, int chunkNo, byte[] body, int port) throws InvalidAttributeValueException {
        if(port < 0)
            return new MessageChunkNo(Protocol.VERSION, MessageType.CHUNK, Peer.getID() + "", fileID, chunkNo + "", body);
        return new MessageChunkNo(Protocol.VERSION, MessageType.CHUNK, Peer.getID() + "", fileID, chunkNo + "", (port + "").getBytes() );
    }

    public static Message createDelete(String fileID)
            throws InvalidAttributeValueException {
        return new Message(Protocol.VERSION, MessageType.DELETE, Peer.getID() + "", fileID, null);
    }

    public static MessageChunkNo createRemoved(String fileID, int chunkNo)
            throws InvalidAttributeValueException {
        return new MessageChunkNo(Protocol.VERSION, MessageType.REMOVED, Peer.getID() + "", fileID, chunkNo + "", null);
    }

    public static SimpleMessage createGreeting() throws InvalidAttributeValueException {
        if(!(Float.parseFloat(Protocol.VERSION) > 1.21))
            throw new InvalidAttributeValueException("Incorrect version: " + Protocol.VERSION);
        return new SimpleMessage(Protocol.VERSION, MessageType.GREETING, Peer.getID() + "");
    }

    public static MessageChunkNo createDelChunk(String fileID, int chunkNo) throws InvalidAttributeValueException {
        if(!(Float.parseFloat(Protocol.VERSION) > 1.21))
            throw new InvalidAttributeValueException("Incorrect version: " + Protocol.VERSION);
        return new MessageChunkNo(Protocol.VERSION, MessageType.DELCHUNK, Peer.getID() + "", fileID, chunkNo + "", null);
    }

}