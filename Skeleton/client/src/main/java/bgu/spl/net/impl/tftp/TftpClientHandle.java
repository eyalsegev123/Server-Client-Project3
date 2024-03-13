package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public class TftpClientHandle {
    public String ClientFilesPath = "/Users/eyalsegev/Documents/Documents - Eyals MacBook Pro/אוניברסיטה /סמסטר ג׳/תכנות מערכות/Server-Client---Project-3/Skeleton/client/ClientFiles/";
    public TftpUtils utils = new TftpUtils();
    public String fileToDownload;
    public TftpClientEncoderDecoder encdec = new TftpClientEncoderDecoder();
    public FileOutputStream fileReader;
    public Socket sock;
    public BufferedInputStream in;
    public BufferedOutputStream out;
    public LinkedList<Byte> uploadFile;
    public LinkedList<byte[]> packetsClient;
    public short blockCounter = 1;
    public String command;
    public LinkedList<Byte> listOfDIRQ;
    public boolean shouldTerminate = false;

    public  boolean existsInClient(String fileName) {
        Path filePathServer = Paths.get(ClientFilesPath, fileName).toAbsolutePath();
        try {
            return Files.exists(filePathServer);
        } catch (InvalidPathException | SecurityException e) {
            return false;
        }
    }

    public void handlePacket(byte[] packet){
        byte[] tempOpcode = new byte[2];
        tempOpcode[0] = packet[0];
        tempOpcode[1] = packet[1];
        short Opcode = utils.byteToShort(tempOpcode);
        if(Opcode == 3){//DATA
            byte[] blockNumber = new byte[2];
            blockNumber[0] = packet[4];
            blockNumber[1] = packet[5];
            short blockNum = utils.byteToShort(blockNumber);
            byte[] data = new byte[packet.length-6];
            for(int i = 0; i < data.length; i++){
                data[i] = packet[i+6];
            }
            byte[] packetSizeBytes = new byte[2];
            packetSizeBytes[0] = packet[2];
            packetSizeBytes[1] = packet[3];
            short packetSize = utils.byteToShort(packetSizeBytes);
            if(command.equals("RRQ")){ //Write to file and send ACK with blockNum
                try {
                    fileReader.write(data);
                    out.write(utils.createACK(blockNum));
                    out.flush();
                } catch (IOException e) {
                }
            }
            else{ //Command is DIRQ -> Print the file names
                for(byte b : data){
                   listOfDIRQ.add(b);
                }
                try {
                    out.write(utils.createACK(blockNum));
                    out.flush();
                } catch (IOException e) {
                }
                if(packetSize < 512){//Last packet 
                    LinkedList<Byte> file = new LinkedList<Byte>();
                    for(byte b : listOfDIRQ){
                        if(b != 0){
                            file.add(b);
                        }
                        else{
                            System.out.println(file.toString());
                            file.clear();
                        }
                    }
                    System.out.println(file.toString()); //Printing the last file (Because no 0 byte at the end)
                }


            }
        }
        else if(Opcode == 4){//ACK   
            byte[] blockNumber = new byte[2];
            blockNumber[0] = packet[2];
            blockNumber[1] = packet[3];
            short blockNum = utils.byteToShort(blockNumber);
            System.out.println("ACK " + blockNum);
            if(command.equals("DISC")){
                shouldTerminate = true;
            }
            else if(blockNum == 0 && command.equals("WRQ")){ //send the first packet when receiving ACK 0
                try {
                    if(packetsClient.isEmpty()){ //The file is an empty file
                        out.write(utils.createData(new byte[0], blockCounter));
                        out.flush();
                    }       
                    else{ //File isnt empty
                        out.write(utils.createData(packetsClient.removeFirst(), blockCounter)); //Start sending packetsClient
                        out.flush();
                        blockCounter++;
                    }
                } catch (IOException e) {
                }
            }
            else if(blockNum != 0 ){ //send another data packet for WRQ
                try {
                    if(packetsClient.isEmpty()){ //We finished to send so we restart blockCounter
                        blockCounter = 1;
                    }   
                    else{
                        out.write(utils.createData(packet, blockCounter));
                        out.flush();
                        blockCounter++;
                    }
                } catch (IOException e) {
                }
                
            }

        }
        else if(Opcode == 9){//BCAST
            byte deleteOrAdd = packet[2];
            byte[] fileName = new byte[packet.length-4];
            for(int i = 0; i < fileName.length; i++){
                fileName[i] = packet[i+3];
            }
            if(deleteOrAdd == 0){
                System.out.println("BCAST del" + fileName.toString());
            }
            else{
                System.out.println("BCAST add" + fileName.toString());
            }
        }
        else if(Opcode == 5){//ERROR //if rrq delete file that we created
            byte[] errorCodeBytes = new byte[2];
            errorCodeBytes[0] = packet[2];
            errorCodeBytes[1] = packet[3];
            short errorCode = utils.byteToShort(errorCodeBytes);
            byte[] errorMsg = new byte[packet.length-5];
            for(int i = 0; i < errorMsg.length; i++){
                errorMsg[i] = packet[i+4];
            }
            if(errorCode == 0){ //Not defined - see error message 
                System.out.println("Error " + errorCode + ": " + errorMsg.toString());
            }
            else if(errorCode == 1){ //File not found - RRQ / DELRQ
                if(command.equals("DELRQ") ){
                    System.out.println("Error " + errorCode + ": " + errorMsg.toString());
                }
                else{ //RRQ command
                    File fileToDelete = new File(ClientFilesPath, fileToDownload);
                    fileToDelete.delete();
                    System.out.println("Error " + errorCode + ": " + errorMsg.toString());
                }
            }
            else if(errorCode == 4){
                System.out.println("Error " + errorCode + ": " + errorMsg.toString());
            }
            else if(errorCode == 5){ //File already exists - WRQ
                System.out.println("Error " + errorCode + ": " + errorMsg.toString());
            }
            else if(errorCode == 6){ //User not logged in - LOGRQ
                System.out.println("Error " + errorCode + ": " + errorMsg.toString());
            }
            else if(errorCode == 7){ //User already logged in - LOGRQ
                System.out.println("Error " + errorCode + ": " + errorMsg.toString());
            }
        }
    }

    public void handleCommand(String command, String name){
        if (command.equals("RRQ")) {
            if (!existsInClient(name)) { // File doesnt exist in client -> we can Read it from server
                try {
                    fileReader = new FileOutputStream(ClientFilesPath + name);
                    out.write(utils.createRRQ(name));
                    out.flush();
                } catch (IOException e) {
                }
                fileToDownload = name;
            } else { // File exists in client -> we can't Read it from server
                System.out.println("File already exists");
            }
        }
        else if (command.equals("WRQ")) {
            if (existsInClient(name)) {
                uploadFile = new LinkedList<Byte>();
                try (FileInputStream in = new FileInputStream(ClientFilesPath + name)) {
                    int read;
                    while ((read = in.read()) != -1) {
                        uploadFile.add((byte) read);
                    }
                } catch (IOException e) {
                }
                byte[] fileRequested = new byte[uploadFile.size()];
                int index = 0;
                for (byte b : uploadFile)
                    fileRequested[index++] = b;
                packetsClient = utils.devideData(fileRequested);
                try {
                    out.write(utils.createWRQ(name));
                    out.flush();
                } catch (IOException e) {
                }
            } else// File doesnt exist in client
                System.out.println("File doesnt exist in client");
        }

        else if (command.equals("DIRQ")) {
            try {
                out.write(utils.createDIRQ());
                out.flush();
                listOfDIRQ = new LinkedList<Byte>();
            } catch (Exception e) {
            }
        }
        else if (command.equals("LOGRQ")) {
            try {
                out.write(utils.createLOGRQ(name));
                out.flush();
            } catch (IOException e) {
            }
        }
        else if (command.equals("DELRQ")) {
            try {
                out.write(utils.createDELRQ(name));
                out.flush();
            } catch (IOException e) {
            }
        }
        else if (command.equals("DISC")) {
            try {
                out.write(utils.createDISC());
                out.flush();
            } catch (IOException e) {
            }
        }
        else{
            try {
                out.write(utils.createUnknown());
                out.flush();
            } catch (IOException e) {
            }
        }
    }

}
