package bgu.spl.net.impl.tftp;

import java.util.LinkedList;
import java.util.List;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private List<Byte> bytes = new LinkedList<Byte>();
    
    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if(nextByte == 0){
            byte[] ret = new byte[bytes.size()];
            int i = 0;
            for(byte b : bytes){
                ret[i] = b;
                i++;
            }
            return ret;
        }
        bytes.add(nextByte);
        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        byte[] toReturn = new byte[message.length+1];
        for(int i = 0; i<message.length; i++)
            toReturn[i] = message[i];
        toReturn[toReturn.length-1] = 0;
        return toReturn;
        
    }
}