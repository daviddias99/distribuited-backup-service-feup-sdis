package task;

import client.Peer;
import subprotocol.BackupSubProtocol;

public class DelayedBackupProtocolTask extends FixedDelayedTask{

    private BackupSubProtocol protocol;

    public DelayedBackupProtocolTask(String fileID, int chunkNo,int delayMS, BackupSubProtocol protocol) {
        super("DELAYED_BACKUP"+ ":" +  protocol.getUID(), delayMS);

        this.protocol = protocol;
    }

    @Override
    protected void taskFunction() {
       
        Peer.getTaskManager().addTask(new SubProtocolTask("DELAYED_BACKUP", protocol));
        this.cancelSelf();
    }

}