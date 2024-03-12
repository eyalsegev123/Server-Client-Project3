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
    private TftpUtils utils;
    
    

    public TftpProtocol(ConcurrentHashMap<Integer,String> LoggedInClients){
        this.LoggedInClients = LoggedInClients;
        blockNumber = 1;
        serverFilesPath = "/Users/eyalsegev/Documents/Documents - Eyals MacBook Pro/אוניברסיטה /סמסטר ג׳/תכנות מערכות/Server-Client---Project-3/Skeleton/server/Files/";
        this.utils = new TftpUtils();
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
        short Opcode = utils.byteToShort(tempOpcode);
        if(!isLoggedIn && Opcode != (short) 7){ //Checks if a user is even loggedin
            connections.send(connectionId, utils.createError((short)6, "User not logged in"));
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
                packets = utils.devideData(fileRequested);
                if(packets.isEmpty())//The file is an empty file
                    connections.send(connectionId, utils.createData(new byte[0], blockNumber));
                else{ //The file isn't
                    connections.send(connectionId, utils.createData(packets.removeFirst(), blockNumber));
                    blockNumber++;
                }    
            }    
            else//File doesnt exist in server
                connections.send(connectionId, utils.createError((short) 1, "File doesn't exist in server"));
        }

        if(Opcode == 2){ //WRQ - Write request
            byte[] fileName = new byte[message.length-3];
            for(int i = 0; i < fileName.length; i++){
                fileName[i] = message[i+2];
            }
            String nameOfFile = new String(fileName, StandardCharsets.UTF_8);
            if(existsInServer(nameOfFile)){ //checking if file exists -> error 
                connections.send(connectionId, utils.createError((short) 5, "File already exists in server"));
            }
            else{ // File doesnt exist
                connections.send(connectionId, utils.createACK((short)0));
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
            short blockNum = utils.byteToShort(blockNumber);
            byte[] data = new byte[message.length-6];
            for(int i = 0; i < data.length; i++){
                data[i] = message[i+6];
            }
            try {
                out.write(data);
                connections.send(connectionId, utils.createACK(blockNum));
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
        if(Opcode == 4){ //ACK packet
            if(packets.isEmpty())
                blockNumber = 1;
            else{
                connections.send(connectionId, utils.createData(packets.removeFirst() , blockNumber));
                blockNumber++;
            }
        }

        if(Opcode == 6){ //DIRQ - Content of Files in server
            String[] fileNames = utils.getFilesNames(serverFilesPath);
            if(fileNames.length > 0){ //The are files in the folder
                String bigFile = "";
                for(int i = 0; i < fileNames.length; i++){ //Putting a 0 byte at the end of each fileName
                    if(fileNames[i] != fileNames[fileNames.length-1]){
                        fileNames[i] = fileNames[i] + "\0"; //adds a 0 between the names of the files
                    }
                    bigFile += fileNames[i]; 
                }
                LinkedList<byte[]> packets = utils.devideData(bigFile.getBytes());
                short blockNumber = 1;
                for (byte[] packet : packets) { 
                    connections.send(connectionId, utils.createData(packet, blockNumber));
                    blockNumber++;
                }
            }
            else //There arent files in folder
                connections.send(connectionId, utils.createError((short) 0, "Files folder is empty"));      
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
                connections.send(connectionId, utils.createACK((short)0));
            }
            else
                connections.send(connectionId, utils.createError((short) 7,"User already logged in"));
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
                connections.send(connectionId, utils.createACK((short)0));
                for(Integer key : LoggedInClients.keySet())
                    connections.send(key, utils.createBCast((short) 0, nameOfFile));
                
            }
            else{
                connections.send(connectionId, utils.createError((short) 1, "File not found"));
            }
        }

        if(Opcode == 10){ //DISC - Disconnect Request
            connections.send(connectionId, utils.createACK((short) 0)); //Send ACK 0 confirmation
            LoggedInClients.remove(connectionId); //Remove from loggedin clients
            connections.disconnect(connectionId); //Removes connectionhandler from connections
            shouldTerminate = true; 
        }
        
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
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

}
