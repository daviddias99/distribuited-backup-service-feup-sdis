package task;

import client.Peer;
import subprotocol.BackupSubProtocol;

public class DelayedReclaimBackupProtocolTask extends RandomDelayedTask{

    private BackupSubProtocol protocol;

    public DelayedReclaimBackupProtocolTask(String fileID, int chunkNo,int maxDelayMS, BackupSubProtocol protocol) {
        super("DELAYED_PROTOCOL_BACKUP"+ ":" +  protocol.getUID(), maxDelayMS);

        this.protocol = protocol;
    }

    @Override
    protected void taskFunction() {
       
        Peer.getTaskManager().addTask(new SubProtocolTask(ID, protocol));
        this.cancelSelf();
    }

}