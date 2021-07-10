package task;

import client.Peer;
import subprotocol.EnhanceDeleteSubProtocol;

public class DelayedEnhanceDeleteProtocolTask extends FixedDelayedTask {

    private EnhanceDeleteSubProtocol protocol;

    public DelayedEnhanceDeleteProtocolTask(int delayMS, EnhanceDeleteSubProtocol protocol) {
        super("DELAYED_PROTOCOL_DELETE"+ ":" +  protocol.getUID(), delayMS);

        this.protocol = protocol;
    }

    @Override
    protected void taskFunction() {
       
        Peer.getTaskManager().addTask(new SubProtocolTask(ID, protocol));
        this.cancelSelf();
    }

}