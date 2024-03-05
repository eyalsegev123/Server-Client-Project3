package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private boolean shouldTerminate;
    private Connections<byte[]> connections;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.shouldTerminate = false;
        this.connections = connections;
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    } 
    
}
