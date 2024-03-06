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
import java.util.concurrent.ConcurrentHashMap;

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
        if(Opcode == 1){ //RRQ - Read request
            connections.send(connectionId, message);
        }
        if(Opcode == 2){ //WRQ - Write request
        //check if connected
        }
        if(Opcode == 6){ //DIRQ - Content of Files in server

        }
        if(Opcode == 7){ //LOGRQ - Login Request
            if(!isLoggedIn){
                isLoggedIn = true;
                LoggedInClients.put(connectionId , new String(message, StandardCharsets.UTF_8));
                connections.send(connectionId, createACK(Opcode));
            }
            else{
                connections.send(connectionId, createError((short) 7));
            }

        }
        if(Opcode == 8){ //DELRQ - Delete Request
            String fileName = new String(message, StandardCharsets.UTF_8);
            if(existsInServer(fileName)){
                String filesDirectory = "/Skeleton/server/Files";
                File fileToDelete = new File(filesDirectory, fileName);
                fileToDelete.delete();
                connections.send(connectionId, createBCast((short) 0, fileName));
                //send ACK
            }
            else{
                //error
            }
                

        }
        if(Opcode == 10){ //DISC - Disconnect Request

        }
        
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
    public byte[] createACK(short Opcode){
        byte[] ACK = new byte[4];
        if(Opcode != 3){
            byte[] bytes = shortToByte((short) 4);
            ACK[0] = bytes[0];
            ACK[1] = bytes[1];
            bytes = shortToByte((short) 0);
            ACK[2] = bytes[2];
            ACK[3] = bytes[3];
        }
        else{
            connections.send(connectionId, createError((short) 7));
        }
        return ACK;
    }

    public byte[] createError(short errorValue){
        byte[] Opcode = shortToByte((short) 5);
        byte[] errorCode = shortToByte(errorValue);
        //change the error messages
        if(errorValue == 0){
            return mergeArrays(Opcode, errorCode, "Not defined error".getBytes());
        }
        if(errorValue == 1){
            return mergeArrays(Opcode, errorCode, "File not found".getBytes());
        }
        if(errorValue == 2){
            return mergeArrays(Opcode, errorCode, "User already logged in".getBytes());
        }
        if(errorValue == 3){
            return mergeArrays(Opcode, errorCode, "User already logged in".getBytes());
        }
        if(errorValue == 4){
            return mergeArrays(Opcode, errorCode, "User already logged in".getBytes());
        }
        if(errorValue == 5){
            return mergeArrays(Opcode, errorCode, "User already logged in".getBytes());
        }
        if(errorValue == 6){
            return mergeArrays(Opcode, errorCode, "User already logged in".getBytes());
        }   
        if(errorValue == 7){
            return mergeArrays(Opcode, errorCode, "User already logged in".getBytes());
        }
        return null;
    }

    public byte[] shortToByte(short a){
        return new byte []{(byte) (a >> 8), (byte) (a & 0xff)};
    }

    public short byteToShort(byte[] bytes){
        return (short) ((( short) bytes [0]) << 8 | (short) (bytes [1]));
    }

    public byte[] mergeArrays(byte[] arr1, byte[] arr2, byte[] arr3){
        
        byte[] output = new byte[arr1.length+arr2.length+arr3.length];
        for(int i = 0; i < arr1.length; i++){
            output[i] = arr1[i];
        }
        for(int i = arr1.length; i < arr1.length + arr2.length ; i++){
            output[i] = arr2[i];
        }
        for(int i = arr1.length + arr2.length; i < output.length; i++){
            output[i] = arr3[i];
        }
        return output;
    }

    public boolean existsInServer(String messageToString){   
        String folderPathServer = "/Skeleton/server/Files"; 
        Path filePathServer = Paths.get(folderPathServer, messageToString);
        return Files.exists(filePathServer);
    }

    // public boolean existsInClient(byte[] message){   
    //     String messageToString = new String(message, StandardCharsets.UTF_8);
    //     String folderPathClient = "/Skeleton/client/ClientFiles"; //We want to see if the file exists in the Client
    //     Path filePathClient = Paths.get(folderPathClient, messageToString);
    //     return Files.exists(filePathClient);
    // }
    
    public byte[] createBCast(short deleteOrAdd, String fileName){
        byte[] Opcode = shortToByte((short) 9);
        byte[] action = shortToByte(deleteOrAdd);
        return mergeArrays(Opcode, action, ("delete " + fileName).getBytes());
    }
}
