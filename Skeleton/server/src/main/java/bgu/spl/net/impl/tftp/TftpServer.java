

package bgu.spl.net.impl.tftp;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.Server;

public class TftpServer {

    public static void main(String[] args) {

        ConcurrentHashMap<Integer, String> LoggedInClients = new ConcurrentHashMap<Integer, String>();

        // you can use any server... 
        Server.threadPerClient(
                7777, //port
                () -> new TftpProtocol(LoggedInClients), //protocol factory
                TftpEncoderDecoder::new //message encoder decoder factory
        ).serve();

    }
}
