package bgu.spl.net.impl.tftp;

import java.io.File;
import java.util.LinkedList;

public class TftpUtils {
    public byte[] createACK(short blockNumber){
        byte[] ACK = new byte[4];
        byte[] bytes = shortToByte((short) 4);
        ACK[0] = bytes[0];
        ACK[1] = bytes[1];
        bytes = shortToByte(blockNumber);
        ACK[2] = bytes[0];
        ACK[3] = bytes[1];
        return ACK;
    }

    public byte[] createError(short errorValue, String errorMsg){
        byte[][] arrays = new byte[4][];
        arrays[0] = shortToByte((short) 5);
        arrays[1] = shortToByte(errorValue);
        arrays[2] = errorMsg.getBytes();
        arrays[3] = new byte[]{0};
        return mergeArrays(arrays);
    }

    public byte[] createBCast(short deleteOrAdd, String fileName){
        byte[][] arrays = new byte[4][];
        arrays[0] = shortToByte((short) 9); //Opcode
        arrays[1] = shortToByte(deleteOrAdd); //Which action 
        arrays[2] = ("delete " + fileName).getBytes(); // Which file is deleted - the msg
        arrays[3] = new byte[]{0};
        return mergeArrays(arrays);
    }

    public byte[] createLOGRQ(String name){
        byte[][] arrays = new byte[3][];
        arrays[0] = shortToByte((short) 7);
        arrays[1] = name.getBytes();
        arrays[2] = new byte[]{0};
        return mergeArrays(arrays);
    }

    public byte[] createData(byte[] packet, short numOfBlock){
        byte[][] arrays = new byte[4][];
        arrays[0] = shortToByte((short) 3);
        arrays[1] = shortToByte((short) packet.length);
        arrays[2] = shortToByte(numOfBlock);
        arrays[3] = packet;
        return mergeArrays(arrays);
    }
    
    public byte[] createRRQ(String name){
        byte[][] arrays = new byte[3][];
        arrays[0] = shortToByte((short) 1);
        arrays[1] = name.getBytes();
        arrays[2]= new byte[]{0};
        return mergeArrays(arrays);
    }
    
    public byte[] createWRQ(String name){
        byte[][] arrays = new byte[3][];
        arrays[0] = shortToByte((short) 2);
        arrays[1] = name.getBytes();
        arrays[2]= new byte[]{0};
        return mergeArrays(arrays);
    }

    public byte[] createDIRQ(){
        return shortToByte((short) 6);
    }

    public byte[] createDELRQ(String name){
        byte[][] arrays = new byte[3][];
        arrays[0] = shortToByte((short) 8);
        arrays[1] = name.getBytes();
        arrays[2]= new byte[]{0};
        return mergeArrays(arrays);
    }

    public byte[] createDISC(){
        return shortToByte((short) 10);
    }

    public byte[] createUnknown(){
        return shortToByte((short) 11);
    }

    public byte[] shortToByte(short a){
        return new byte []{(byte) (a >> 8), (byte) (a & 0xff)};
    }

    public short byteToShort(byte[] bytes){
        return (short) (((short) bytes[0]) << 8 | (short) (bytes[1]) & 0x00ff);
    }

    public byte[] mergeArrays(byte[][] arrays){
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        byte[] result = new byte[totalLength];
        int currentIndex = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }

        return result;
    }
    
    public String[] getFilesNames(String folderPath){
        File folder = new File(folderPath);
        String[] filenames = folder.list();
        return filenames != null ? filenames : new String[0]; //If filenames is null we will get an empty array of Strings
    }

    public LinkedList<byte[]> devideData(byte[] bigFile){
        LinkedList<byte[]> output = new LinkedList<byte[]>();
        int length = bigFile.length;
        int j = 0;
        int lastPacketSize = 0;
        while(length > 0){
            byte[] arr;
            if(length > 512){
                arr = new byte[512];
                length -= 512;
            }
            else{ //The length left isnt greater than 512
                arr = new byte[length];
                length = 0;
                lastPacketSize = length;
            }
            for(int i = 0; i < arr.length; i++){
                    arr[i] = bigFile[j];
                    j++;
            }
            output.add(arr);
        }
        if(lastPacketSize == 512){ //In case that lastpacket is exactly 512 long -> add a empty packet
            output.add(new byte[0]);
        }
        return output;
    } 

}
