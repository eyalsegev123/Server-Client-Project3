package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class TftpClient {

    

    public static void main(String[] args) {

        TftpClientHandle clientHandler = new TftpClientHandle();

        try {
            clientHandler.sock = new Socket("127.0.0.1",7777);
            Thread listening = new Thread(() -> {
                try {
                    clientHandler.in = new BufferedInputStream(clientHandler.sock.getInputStream());
                    int read;
                    while (!clientHandler.shouldTerminate && (read = clientHandler.in.read()) >= 0) {
                        byte[] nextMessage = clientHandler.encdec.decodeNextByte((byte) read);
                        if (nextMessage != null) {
                            clientHandler.handlePacket(nextMessage); // send func take cares of encoding
                        }
                    }
                } catch (IOException e) {

                }

            });

            Thread keyboard = new Thread(() -> {
                try {
                    clientHandler.out = new BufferedOutputStream(clientHandler.sock.getOutputStream());
                    Scanner scan = new Scanner(System.in);
                    while(!clientHandler.shouldTerminate){
                        String userInput = scan.nextLine();
                        String[] userInputSplit = userInput.split(" ");
                        clientHandler.command = userInputSplit[0];
                        String name = "";
                        if(userInputSplit.length > 1){ //We have another parameter
                            name = userInputSplit[1];
                        }
                        clientHandler.handleCommand(clientHandler.command, name);
                    }
                    scan.close();
                } catch (IOException e) {

                }

            });

            listening.start();
            keyboard.start();

        } catch (IOException e) {
        }

    }


}
