package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Scanner;

public class TftpClient {

    public static String ClientFilesPath = "/Users/eyalsegev/Documents/Documents - Eyals MacBook Pro/אוניברסיטה /סמסטר ג׳/תכנות מערכות/Server-Client---Project-3/Skeleton/client/ClientFiles/";
    public static TftpUtils utils = new TftpUtils();
    public static String fileToDownload;
    public static TftpClientEncoderDecoder encdec = new TftpClientEncoderDecoder();
    public static FileOutputStream fileReader;
    public static Socket sock;
    public static BufferedInputStream in;
    public static BufferedOutputStream out;
    public static LinkedList<Byte> uploadFile;
    public static LinkedList<byte[]> packets;
    public static short blockCounter = 1;
    public static String command;
    public static LinkedList<Byte> listOfDIRQ;
    public static boolean shouldTerminate = false;

    public static void main(String[] args) {

        try {
            sock = new Socket(args[0], 7777);
            Thread listening = new Thread(() -> {
                try {
                    in = new BufferedInputStream(sock.getInputStream());
                    int read;
                    while (!shouldTerminate && (read = in.read()) >= 0) {
                        byte[] nextMessage = encdec.decodeNextByte((byte) read);
                        if (nextMessage != null) {
                            handlePacket(nextMessage); // send func take cares of encoding
                        }
                    }
                    // delete and reset the fileToDownload field
                } catch (IOException e) {

                }

            });

            Thread keyboard = new Thread(() -> {
                try {
                    out = new BufferedOutputStream(sock.getOutputStream());
                    Scanner scan = new Scanner(System.in);
                    while(!shouldTerminate){
                        String userInput = scan.nextLine();
                        String[] userInputSplit = userInput.split(" ");
                        command = userInputSplit[0];
                        String name = userInputSplit[1];
                        handleCommand(command, name);
                    }
                    scan.close();
                } catch (IOException e) {

                }

            });

        } catch (IOException e) {
        }

    }

    public static boolean existsInClient(String fileName) {
        Path filePathServer = Paths.get(ClientFilesPath, fileName).toAbsolutePath();
        try {
            return Files.exists(filePathServer);
        } catch (InvalidPathException | SecurityException e) {
            return false;
        }
    }

    public static void handlePacket(byte[] packet){
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
                } catch (IOException e) {
                }
            }
            else{ //Command is DIRQ -> Print the file names
                for(byte b : data){
                   listOfDIRQ.add(b);
                }
                try {
                    out.write(utils.createACK(blockNum));
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
        if(Opcode == 4){//ACK   
            byte[] blockNumber = new byte[2];
            blockNumber[0] = packet[2];
            blockNumber[1] = packet[3];
            short blockNum = utils.byteToShort(blockNumber);
            System.out.println("ACK" + blockNum);
            if(command.equals("DISC")){
                shouldTerminate = true;
            }
            if(blockNum == 0 && command.equals("WRQ")){ //send the first packet when receiving ACK 0
                try {
                    if(packets.isEmpty()){ //The file is an empty file
                        out.write(utils.createData(new byte[0], blockCounter));
                    }       
                    else{ //File isnt empty
                        out.write(utils.createData(packets.removeFirst(), blockCounter)); //Start sending packets
                        blockCounter++;
                    }
                } catch (IOException e) {
                }
            }
            else if(blockNum != 0 ){ //send another data packet for WRQ
                try {
                    if(packets.isEmpty()){ //We finished to send so we restart blockCounter
                        blockCounter = 1;
                    }   
                    else{
                        out.write(utils.createData(packet, blockCounter));
                        blockCounter++;
                    }
                } catch (IOException e) {
                }
                
            }

        }
        if(Opcode == 9){//BCAST
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
        if(Opcode == 5){//ERROR //if rrq delete file that we created
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

    public static void handleCommand(String command, String name){
        if (command.equals("RRQ")) {
            if (!existsInClient(name)) { // File doesnt exist in client -> we can Read it from server
                try {
                    fileReader = new FileOutputStream(ClientFilesPath + name);
                } catch (IOException e) {
                }
                out.write(utils.createRRQ(name));
                fileToDownload = name;
            } else { // File exists in client -> we can't Read it from server
                System.out.println("File already exists");
            }
        }
        if (command.equals("WRQ")) {
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
                packets = utils.devideData(fileRequested);
                try {
                    out.write(utils.createWRQ(name));
                } catch (IOException e) {
                }
            } else// File doesnt exist in client
                System.out.println("File doesnt exist in client");
        }

        if (command.equals("DIRQ")) {
            try {
                out.write(utils.createDIRQ());
                listOfDIRQ = new LinkedList<Byte>();
            } catch (Exception e) {
            }
        }
        if (command.equals("LOGRQ")) {
            try {
                out.write(utils.createLOGRQ(name));
            } catch (IOException e) {
            }
        }
        if (command.equals("DELRQ")) {
            try {
                out.write(utils.createDELRQ(name));
            } catch (IOException e) {
            }
        }
        if (command.equals("DISC")) {
            try {
                out.write(utils.createDISC());
            } catch (IOException e) {
            }
        }
        else{
            try {
                out.write(utils.createUnknown());
            } catch (IOException e) {
            }
        }
    }

}
