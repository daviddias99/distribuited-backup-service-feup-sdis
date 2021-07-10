package message;

public enum MessageType {

    PUTCHUNK("PUTCHUNK"), 
    STORED("STORED"), 
    ALREADY_STORED("ALREADY_STORED"), 
    ENHANCED_STORED("ENHANCED_STORED"), 
    GETCHUNK("GETCHUNK"), 
    CHUNK("CHUNK"), 
    DELETE("DELETE"), 
	REMOVED("REMOVED"),
	GREETING("GREETING"),
	DELCHUNK("DELCHUNK");

	private String type;

	private MessageType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return type;
	}

}