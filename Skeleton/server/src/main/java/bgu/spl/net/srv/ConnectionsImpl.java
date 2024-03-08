package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl implements Connections<byte[]>{

    private ConcurrentHashMap<Integer, ConnectionHandler<byte[]>> holder;

    public ConnectionsImpl(){
        this.holder = new ConcurrentHashMap<Integer, ConnectionHandler<byte[]>>();
    }
   
    @Override
    public void connect(int connectionId, ConnectionHandler<byte[]> handler){
        holder.put(connectionId, handler);
    }

    public boolean send(int connectionId, byte[] msg){
        ConnectionHandler<byte[]> sender = holder.get(connectionId);
        if(sender != null){
            sender.send(msg);
            return true;
        }
        return false;  
    }

    public void disconnect(int connectionId){
        holder.remove(connectionId);
    }

    
}
