package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private boolean shouldTerminate;
    private Connections<byte[]> connections;
    //holder for BCAST
    
    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.shouldTerminate = false;
        this.connections = connections;
    }

    @Override
    public void process(byte[] message, int Opcode) {
        if(Opcode == 1){ //RRQ - Read request
            connections.send(connectionId, message);
        }
        if(Opcode == 2){ //WRQ - Write request

        }
        if(Opcode == 3){ //DATA

        }
        if(Opcode == 4){ //ACK - Acknowledgement

        }
        if(Opcode == 5){ //ERROR

        }
        if(Opcode == 6){ //DIRQ

        }
        if(Opcode == 7){ //LOGRQ - Login Request

        }
        if(Opcode == 8){ //DELRQ - Delete Request

        }
        if(Opcode == 9){ //BCAST - 

        }
        if(Opcode == 'a'){ //RRQ - Read request

        }
        
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    } 
    
}
