import java.net.*;
import java.io.*;
import java.nio.*;

public class ProxyTriple {
    public String host;
    public int port;
    public int weight;
    public String range;

    public ProxyTriple(String host, int port, int weight, String range){
        this.host = host;
        this.port = port;
        this.weight = weight;
        this.range = range;
    }
}