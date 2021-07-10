package subprotocol;

import javax.naming.directory.InvalidAttributeValueException;

import client.Peer;
import message.MessageChunkNo;
import message.MessageType;
import utils.Protocol;


public class ReclaimSubProtocol extends SubProtocol {
    private MessageChunkNo message;

    public ReclaimSubProtocol(String fileID, String chunkNo) throws InvalidAttributeValueException {
        message = new MessageChunkNo(Protocol.VERSION, MessageType.REMOVED, Peer.getID() + "", fileID, chunkNo, null);
    }

    @Override
    public void run() {
        Peer.getControlChannel().sendMessage(message.getBytes());
        System.out.println("Reclaim protocol of file " + this.getUID() + ", ended sucessfully");
    }

    @Override
    public String getUID() {
        return message.getFileID() + ":" + message.getChunkNo();
    }

}