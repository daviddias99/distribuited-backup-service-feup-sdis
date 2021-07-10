package message;

import java.util.Arrays;

import javax.naming.directory.InvalidAttributeValueException;
import utils.Protocol;


public class MessageParser {

    public static SimpleMessage parse(byte[] data, int dataLength) throws InvalidAttributeValueException {
        int sep = getCRLFIndex(data);
        if(sep < 0)
            throw new InvalidAttributeValueException("Message header does not end with <CRLF><CRLF>");

        String header = new String(Arrays.copyOfRange(data, 0, sep)).trim();
        String[] headerParam = header.split(" ");
        byte[] body = Arrays.copyOfRange(data, sep + 4, dataLength);

        MessageType messageType = MessageType.valueOf(headerParam[1].trim());

        switch (messageType) {
            case PUTCHUNK:
                return new MessageRepDeg(headerParam[0].trim(), messageType, headerParam[2].trim(), headerParam[3].trim(), headerParam[4].trim(), headerParam[5].trim(), body);
            case STORED:
                return new MessageChunkNo(headerParam[0].trim(), messageType, headerParam[2].trim(), headerParam[3].trim(), headerParam[4].trim(), body);
            case GETCHUNK:
                return new MessageChunkNo(headerParam[0].trim(), messageType, headerParam[2].trim(), headerParam[3].trim(), headerParam[4].trim(), body);
            case CHUNK:
                return new MessageChunkNo(headerParam[0].trim(), messageType, headerParam[2].trim(), headerParam[3].trim(), headerParam[4].trim(), body);
            case DELETE:
                return new Message(headerParam[0].trim(), messageType, headerParam[2].trim(), headerParam[3].trim(), body);
            case REMOVED:
                return new MessageChunkNo(headerParam[0].trim(), messageType, headerParam[2].trim(), headerParam[3].trim(), headerParam[4].trim(), body);
            case GREETING:
                return new SimpleMessage(headerParam[0].trim(), messageType, headerParam[2].trim());
            case DELCHUNK:
                return new MessageChunkNo(headerParam[0].trim(), messageType, headerParam[2].trim(), headerParam[3].trim(), headerParam[4].trim(), body);
            default:
                throw new InvalidAttributeValueException("Invalid message type");
        }       
    }


    private static int getCRLFIndex(byte[] message){
        for(int i = 0; i < message.length - 3; i++)
            if(message[i] == Protocol.CR && message[i + 1] == Protocol.LF && message[i + 2] == Protocol.CR && message[i + 3] == Protocol.LF)
                return i;
        return -1;
    }
}
