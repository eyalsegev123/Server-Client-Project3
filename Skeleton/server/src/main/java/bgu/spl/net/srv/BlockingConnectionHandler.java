package bgu.spl.net.srv;

import bgu.spl.net.impl.tftp.TftpEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<byte[]> {

    private final TftpProtocol protocol;
    private final TftpEncoderDecoder encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public BlockingConnectionHandler(Socket sock, TftpEncoderDecoder reader, TftpProtocol protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        // start of protocol happens here in run
        try (Socket sock = this.sock) { //just for automatic closing
            //protocol.start(0, null);
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                byte[] nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage, encdec.getOpcode());
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(byte[] msg) {
        try 
        {
            if (msg != null) 
            {
                out.write(encdec.encode(msg));
                out.flush();
            }     
        } 
        catch (IOException ex) 
        {
        ex.printStackTrace();
        }
        
    }
}
