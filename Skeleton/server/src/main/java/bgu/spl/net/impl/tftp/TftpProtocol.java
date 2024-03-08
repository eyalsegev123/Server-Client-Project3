package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private boolean shouldTerminate;
    private Connections<byte[]> connections; //connected Clients
    private boolean isLoggedIn = false;
    private ConcurrentHashMap<Integer,String> LoggedInClients;

    public TftpProtocol(ConcurrentHashMap<Integer,String> LoggedInClients){
        this.LoggedInClients = LoggedInClients;
    }

    
    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.shouldTerminate = false;
        this.connections = connections;

    }

    @Override
    public void process(byte[] message) {
        byte[] tempOpcode = new byte[2];
        tempOpcode[0] = message[0];
        tempOpcode[1] = message[1];
        short Opcode = byteToShort(tempOpcode);
        if(!isLoggedIn && Opcode != (short) 7){ //Checks if a user is even loggedin
            connections.send(connectionId, createError((short)6));
            return;
        }
        if(Opcode == 1){ //RRQ - Read request
            connections.send(connectionId, message);
        }
        if(Opcode == 2){ //WRQ - Write request
        //check if connected
        }
        if(Opcode == 6){ //DIRQ - Content of Files in server
            String[] fileNames = getFilesNames("Skeleton/server/Files");
            String bigFile = "";
            for(String s : fileNames){ //Putting a 0 byte at the end of each fileName
                if(s != fileNames[fileNames.length-1]){
                    s += "\0";
                }
                bigFile += s; 
            }
            LinkedList<byte[]> packets = devideData(bigFile.getBytes());
            short blockNumber = 1;
            for (byte[] packet : packets) { 
                connections.send(connectionId, createData(packet, blockNumber));
                blockNumber++;
            }
            //else send error
            
        }
        if(Opcode == 7){ //LOGRQ - Login Request
        //forum
            String userName = new String(message, StandardCharsets.UTF_8); //userName sent with command
            if(!isLoggedIn && !userExists(userName)){ //Checks if client logged in for the first time and if username exists in System
                isLoggedIn = true;
                LoggedInClients.put(connectionId ,userName); 
                connections.send(connectionId, createACK((short)0));
            }
            else
                connections.send(connectionId, createError((short) 7));
        }

        if(Opcode == 8){ //DELRQ - Delete Request
            String fileName = new String(message, StandardCharsets.UTF_8); //fileName sent with command
            if(existsInServer(fileName)){ //true iff the file exists in server
                String filesDirectory = "/Skeleton/server/Files";
                File fileToDelete = new File(filesDirectory, fileName);
                fileToDelete.delete();
                connections.send(connectionId, createACK((short)0));
                for(Integer key : LoggedInClients.keySet())
                    connections.send(key, createBCast((short) 0, fileName));
                
            }
            else{
                connections.send(connectionId, createError((short) 1));
            }
        }

        if(Opcode == 10){ //DISC - Disconnect Request
            LoggedInClients.remove(connectionId); //Remove from loggedin clients
            connections.disconnect(connectionId); //Disconnect from server 
            connections.send(connectionId, createACK((short) 0)); //Send ACK 0 confirmation
            shouldTerminate = true; //
        }
        
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
    public byte[] createACK(short blockNumber){
        byte[] ACK = new byte[4];
        byte[] bytes = shortToByte((short) 4);
        ACK[0] = bytes[0];
        ACK[1] = bytes[1];
        bytes = shortToByte((short) blockNumber);
        ACK[2] = bytes[0];
        ACK[3] = bytes[1];
        return ACK;
    }

    public byte[] createError(short errorValue){
        byte[][] arrays = new byte[3][];
        arrays[0] = shortToByte((short) 5);
        arrays[1] = shortToByte(errorValue);
        //change the error messages
        if(errorValue == 0){
            arrays[3] = "Not defined error".getBytes();
            return mergeArrays(arrays);
        }
        if(errorValue == 1){
            arrays[3] = "File not found".getBytes();
            return mergeArrays(arrays);
        }
        if(errorValue == 2){
            arrays[3] = "Not defined error".getBytes();
            return mergeArrays(arrays);
        }
        if(errorValue == 3){
            arrays[3] = "Not defined error".getBytes();
            return mergeArrays(arrays);
        }
        if(errorValue == 4){
            arrays[3] = "Not defined error".getBytes();
            return mergeArrays(arrays);
        }
        if(errorValue == 5){
            arrays[3] = "Not defined error".getBytes();
            return mergeArrays(arrays);
        }
        if(errorValue == 6){
            arrays[3] = "User not logged in".getBytes();
            return mergeArrays(arrays);
        }   
        if(errorValue == 7){
            arrays[3] = "User already logged in".getBytes();
            return mergeArrays(arrays);
        }
        return null;
    }

    public byte[] createBCast(short deleteOrAdd, String fileName){
        byte[][] arrays = new byte[3][];
        arrays[0] =  shortToByte((short) 9); //Opcode
        arrays[1] = shortToByte(deleteOrAdd); //Which action 
        arrays[2] = ("delete " + fileName).getBytes(); // Which file is deleted - the msg
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

    public byte[] shortToByte(short a){
        return new byte []{(byte) (a >> 8), (byte) (a & 0xff)};
    }

    public short byteToShort(byte[] bytes){
        return (short) ((( short) bytes [0]) << 8 | (short) (bytes [1]));
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
    
    public boolean existsInServer(String messageToString){   
        String folderPathServer = "/Skeleton/server/Files"; 
        Path filePathServer = Paths.get(folderPathServer, messageToString);
        return Files.exists(filePathServer);
    }

    public boolean userExists(String username){
        for (String user : LoggedInClients.values()) {
            if (user.equals(username)){
                return true;
            }
        }
        return false;
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
        while(length > 0){
            byte[] arr;
            if(length > 512){
                arr = new byte[512];
                length -= 512;
            }
            else{ //The length left isnt greater than 512
                arr = new byte[length];
                length = 0;
            }
            for(int i = 0; i < arr.length; i++){
                    arr[i] = bigFile[j];
                    j++;
            }
            output.add(arr);
        }
        return output;
    } 

    // public boolean existsInClient(byte[] message){   
    //     String messageToString = new String(message, StandardCharsets.UTF_8);
    //     String folderPathClient = "/Skeleton/client/ClientFiles"; //We want to see if the file exists in the Client
    //     Path filePathClient = Paths.get(folderPathClient, messageToString);
    //     return Files.exists(filePathClient);
    // }   
    
}
