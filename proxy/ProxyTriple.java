import java.net.*;
import java.io.*;
import java.nio.*;

public class ProxyTriple {
    public String host;
    public int port;
    public int weight;

    public ProxyTriple(String host, int port, int weight){
        this.host = host;
        this.port = port;
        this.weight = weight;
    }
}