package subprotocol;

import javax.naming.directory.InvalidAttributeValueException;

import client.Peer;
import message.Message;
import message.MessageFactory;

public class DeleteSubProtocol extends SubProtocol {

    private Message message;

    public DeleteSubProtocol(String fileID) throws InvalidAttributeValueException {

        // Create the backup message
        message = MessageFactory.createDelete(fileID);

        // Remove own the file from the backup system (??)
        Peer.getBackupSystem().removeFile(message.getFileID());
    }

    @Override
    public void run() {
        Peer.getControlChannel().sendMessage(message.getBytes());
        Peer.getTaskManager().cancelTask("SUB_PROTOCOL_DELETE"+ ":" +  this.getUID());
        System.out.println("Delete protocol of file " + message.getFileID() + ", ended successfully");
    }

    @Override
    public String getUID() {

        return message.getFileID();
    }

}