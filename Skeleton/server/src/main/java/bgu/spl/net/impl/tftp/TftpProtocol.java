package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;


public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private boolean shouldTerminate;
    private Connections<byte[]> connections; //connected Clients
    private boolean isLoggedIn = false;
    private ConcurrentHashMap<Integer,String> LoggedInClients;
    private FileOutputStream out;
    private LinkedList<byte[]> packets;
    private short blockNumber;
    private String serverFilesPath;
    
    

    public TftpProtocol(ConcurrentHashMap<Integer,String> LoggedInClients){
        this.LoggedInClients = LoggedInClients;
        blockNumber = 1;
        serverFilesPath = "/Users/eyalsegev/Documents/Documents - Eyals MacBook Pro/אוניברסיטה /סמסטר ג׳/תכנות מערכות/Server-Client---Project-3/Skeleton/server/Files/";
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
            connections.send(connectionId, createError((short)6, "User not logged in"));
            return;
        }
        if(Opcode == 1){ //RRQ - Read request
            
            byte[] fileName = new byte[message.length-3];
            for(int i = 0; i < fileName.length; i++){
                fileName[i] = message[i+2];
            }
            String nameOfFile = new String(fileName, StandardCharsets.UTF_8);
            if(existsInServer(nameOfFile)){  // checking if server has file
                LinkedList<Byte> fileBytesRequested = new LinkedList<Byte>(); 
                try(FileInputStream in = new FileInputStream(serverFilesPath + nameOfFile)) { 
                    //Reading bytes from files with FileInputStream
                    int byteRead;
                    while((byteRead = in.read()) != -1){
                        fileBytesRequested.add((byte) byteRead);
                    }
                } catch (IOException e) {
                }
                byte[] fileRequested = new byte[fileBytesRequested.size()];
                int index = 0;
                for (byte b : fileBytesRequested) 
                    fileRequested[index++] = b;
                packets = devideData(fileRequested);
                if(packets.isEmpty())//The file is an empty file
                    connections.send(connectionId, createData(new byte[0], blockNumber));
                else{ //The file isn't
                    connections.send(connectionId, createData(packets.removeFirst(), blockNumber));
                    blockNumber++;
                }    
            }    
            else//File doesnt exist in server
                connections.send(connectionId, createError((short) 1, "File doesn't exist in server"));
        }

        if(Opcode == 2){ //WRQ - Write request
            byte[] fileName = new byte[message.length-3];
            for(int i = 0; i < fileName.length; i++){
                fileName[i] = message[i+2];
            }
            String nameOfFile = new String(fileName, StandardCharsets.UTF_8);
            if(existsInServer(nameOfFile)){ //checking if file exists -> error 
                connections.send(connectionId, createError((short) 5, "File already exists in server"));
            }
            else{ // File doesnt exist
                connections.send(connectionId, createACK((short)0));
                try {
                    out = new FileOutputStream(serverFilesPath + nameOfFile); //Creating a new stream to recieve the upload of the client
                } catch (IOException e) {
                }
            }
        }

        if(Opcode == 3){//DATA packet for uploading a file from the client to the server
            byte[] blockNumber = new byte[2];
            blockNumber[0] = message[4];
            blockNumber[1] = message[5];
            short blockNum = byteToShort(blockNumber);
            byte[] data = new byte[message.length-6];
            for(int i = 0; i < data.length; i++){
                data[i] = message[i+6];
            }
            try {
                out.write(data);
                connections.send(connectionId, createACK(blockNum));
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
        if(Opcode == 4){ //ACK packet
            if(packets.isEmpty())
                blockNumber = 1;
            else{
                connections.send(connectionId, createData(packets.removeFirst() , blockNumber));
                blockNumber++;
            }
        }

        if(Opcode == 6){ //DIRQ - Content of Files in server
            String[] fileNames = getFilesNames(serverFilesPath);
            if(fileNames.length > 0){ //The are files in the folder
                String bigFile = "";
                for(int i = 0; i < fileNames.length; i++){ //Putting a 0 byte at the end of each fileName
                    if(fileNames[i] != fileNames[fileNames.length-1]){
                        fileNames[i] = fileNames[i] + "\0"; //adds a 0 between the names of the files
                    }
                    bigFile += fileNames[i]; 
                }
                LinkedList<byte[]> packets = devideData(bigFile.getBytes());
                short blockNumber = 1;
                for (byte[] packet : packets) { 
                    connections.send(connectionId, createData(packet, blockNumber));
                    blockNumber++;
                }
            }
            else //There arent files in folder
                connections.send(connectionId, createError((short) 0, "Files folder is empty"));      
        }
        if(Opcode == 7){ //LOGRQ - Login Request
            byte[] userName = new byte[message.length-3];
            for(int i = 0; i < userName.length; i++){
                userName[i] = message[i+2];
            }
            String name = new String(userName, StandardCharsets.UTF_8);            
            if(!isLoggedIn && !userExists(name)){ //Checks if client logged in for the first time and if username exists in System
                isLoggedIn = true;
                LoggedInClients.put(connectionId ,name); 
                connections.send(connectionId, createACK((short)0));
            }
            else
                connections.send(connectionId, createError((short) 7,"User already logged in"));
        }

        if(Opcode == 8){ //DELRQ - Delete Request
            byte[] fileName = new byte[message.length-3];
            for(int i = 0; i < fileName.length; i++){
                fileName[i] = message[i+2];
            }
            String nameOfFile = new String(fileName, StandardCharsets.UTF_8); //fileName sent with command
            if(existsInServer(nameOfFile)){ //true iff the file exists in server
                String filesDirectory = serverFilesPath;
                File fileToDelete = new File(filesDirectory, nameOfFile);
                fileToDelete.delete();
                connections.send(connectionId, createACK((short)0));
                for(Integer key : LoggedInClients.keySet())
                    connections.send(key, createBCast((short) 0, nameOfFile));
                
            }
            else{
                connections.send(connectionId, createError((short) 1, "File not found"));
            }
        }

        if(Opcode == 10){ //DISC - Disconnect Request
            connections.send(connectionId, createACK((short) 0)); //Send ACK 0 confirmation
            LoggedInClients.remove(connectionId); //Remove from loggedin clients
            connections.disconnect(connectionId); //Removes connectionhandler from connections
            shouldTerminate = true; 
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
    
    public boolean existsInServer(String fileName){   
        Path filePathServer = Paths.get(serverFilesPath, fileName).toAbsolutePath();
        try {
            return Files.exists(filePathServer);
        } catch (InvalidPathException | SecurityException e) {
        return false;
        }
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

    // public boolean existsInClient(byte[] message){   
    //     String messageToString = new String(message, StandardCharsets.UTF_8);
    //     String folderPathClient = "/Skeleton/client/ClientFiles"; //We want to see if the file exists in the Client
    //     Path filePathClient = Paths.get(folderPathClient, messageToString);
    //     return Files.exists(filePathClient);
    // }   
    
}
