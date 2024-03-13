package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpClientEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    public short Opcode = -1;
    public int packetSize;


    @Override
    public byte[] decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if(len < 2){ //Opcode always goes in
            pushByte(nextByte);
            if(len == 2){ //Defining the Opcode
                byte[] OpcodeBytes = new byte[2];
                OpcodeBytes[0] = bytes[0];
                OpcodeBytes[1] = bytes[1]; 
                Opcode = byteToShort(OpcodeBytes);
                return null;
            }
        }
        if(Opcode != -1){ //Opcode is set to a case
            switch(Opcode){
                case 3: //DATA
                    if(len < 6){
                        pushByte(nextByte);
                        if(len == 4){ //Defining packetSize
                            byte[] packetSizeBytes = new byte[2];
                            packetSizeBytes[0] = bytes[2];
                            packetSizeBytes[1] = bytes[3]; 
                            packetSize = byteToShort(packetSizeBytes);
                        }
                    }    
                    else {
                        pushByte(nextByte);
                    }
                    if(len == packetSize + 6){ //Finished the whole packet -> return the packet
                        return popByte();
                    }
                    return null;
                    
                case 4://ACK
                    pushByte(nextByte);
                    if(len == 4){
                        return popByte();
                    }  
                    return null;

                case 6://BCast    
                    if(len < 3){
                        pushByte(nextByte);
                        return null;
                    }
                    if(nextByte == 0){
                        pushByte(nextByte);
                        return popByte();
                    }
                    pushByte(nextByte);
                    return null;

                case 5: //Error
                    if(len < 4){
                        pushByte(nextByte);
                        return null;
                    }
                    if(nextByte == 0){
                        pushByte(nextByte);
                        return popByte();
                    }
                    pushByte(nextByte);
                    return null;    
            }
        }
        return null;
        
        
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
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
        Opcode = -1;
        return result;
    }

    public short byteToShort(byte[] bytes){
        return (short) (((short) bytes[0]) << 8 | (short) (bytes[1]) & 0x00ff);
    }

}