package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
    public static TftpClientEncoderDecoder encdec;
    public static FileOutputStream fileReader;
    public static Socket sock;
    public static BufferedInputStream in;
    public static BufferedOutputStream out;
    public static LinkedList<Byte> uploadFile;
    public static LinkedList<byte[]> packets;
    public static short blockCounter = 1;

    public static void main(String[] args) {
       
        try {
            sock = new Socket(args[0],7777);
            Thread listening = new Thread(()-> {
                try {
                    in = new BufferedInputStream(sock.getInputStream());
                    int read;
                    while (!protocol.shouldTerminate() && (read = in.read()) >= 0) {
                        byte[] nextMessage = encdec.decodeNextByte((byte) read);
                        if (nextMessage != null) {
                            handlePacket(nextMessage); //send func take cares of encoding
                        }
                    }
                    //delete and reset the fileToDownload field
                } catch (IOException e) {
                    
                }
                
            }
            );

            Thread keyboard = new Thread(()-> {
                try {
                    out = new BufferedOutputStream(sock.getOutputStream());
                    Scanner scan = new Scanner(System.in);
                    String userInput = scan.nextLine(); 
                    String[] userInputSplit = userInput.split(" ");
                    String command = userInputSplit[0];
                    String name = userInputSplit[1];
                    if(command.equals("RRQ")){
                        if(!existsInClient(name)){ //File doesnt exist in client -> we can Read it from server
                            try{
                                fileReader = new FileOutputStream(ClientFilesPath + name);
                            } catch (IOException e) {
                            }
                            out.write(utils.createRRQ(name));
                            fileToDownload = name;
                        }
                        else{ //File exists in client -> we can't Read it from server
                            System.out.println("File already exists");
                        }
                    }
                    if(command.equals("WRQ")){
                        if(existsInClient(name)){
                            uploadFile = new LinkedList<Byte>();
                            try (FileInputStream in = new FileInputStream(ClientFilesPath + name)){
                                int read;
                                while((read = in.read()) != -1){
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
                        }    
                        else//File doesnt exist in client
                            System.out.println("File doesnt exist in client");
                    }

                    if(command.equals("DIRQ")){
                        
                    }
                    if(command.equals("LOGRQ")){
                        
                    }
                    if(command.equals("DELRQ")){
                        
                    }
                    if(command.equals("DISC")){
                        
                    }
                } catch (IOException e) {
                    
                }
                    
            }
            );          
                
            
            
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
        int packetSize;
        short errorCode;
        byte addOrRemove;
        if(Opcode == 3){//DATA
            byte[] blockNumber = new byte[2];
            blockNumber[0] = packet[4];
            blockNumber[1] = packet[5];
            short blockNum = utils.byteToShort(blockNumber);
            byte[] data = new byte[packet.length-6];
            for(int i = 0; i < data.length; i++){
                data[i] = packet[i+6];
            }
            try {
                fileReader.write(data);
                out.write(utils.createACK(blockNum));
            } catch (IOException e) {
            }
            System.out.println("ACK " + blockNum);

        }
        if(Opcode == 4){//ACK   
            byte[] blockNumber = new byte[2];
            blockNumber[0] = packet[2];
            blockNumber[1] = packet[3];
            short blockNum = utils.byteToShort(blockNumber);
            System.out.println("ACK" + blockNum);
            //how can we understand that we are in the WRQ case
            if(blockNum != 0){//send another data packet for WRQ
                try {
                    if(packets.isEmpty())
                        blockCounter = 1;
                    else{
                        out.write(utils.createData(packet, blockNum));
                        blockCounter++;
                    }
                } catch (IOException e) {
                }
                
            }

        }
        if(Opcode == 9){//BCAST
        
        }
        if(Opcode == 5){//ERROR
        
        }
    }

   
}
