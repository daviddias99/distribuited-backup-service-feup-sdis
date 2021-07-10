package task;

import subprotocol.SubProtocol;

public class SubProtocolTask extends Task {

    SubProtocol subprotocol;

    public SubProtocolTask(String ID, SubProtocol protocol) {
        super("SUB_PROTOCOL_" + ID + ":" + protocol.getUID());
        this.subprotocol = protocol;
    }

    @Override
    protected void taskFunction() {
        
        this.subprotocol.run();
        this.cancelSelf();
    }

}