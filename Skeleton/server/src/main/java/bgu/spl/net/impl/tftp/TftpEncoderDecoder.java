package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if(nextByte == 0){
            pushByte(nextByte);
            return popByte();
        }

        pushByte(nextByte);
        return null; //not a full message yet
    }

    @Override
    public byte[] encode(byte[] message) {
        byte[] toReturn = Arrays.copyOf(message, message.length+1);
        toReturn[toReturn.length-1] = 0;
        return toReturn;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }

    private byte[] popByte() {
        byte[] result = Arrays.copyOf(bytes, len);
        len = 0;
        return result;
    }

}