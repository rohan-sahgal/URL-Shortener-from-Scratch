import java.net.*;
import java.io.*;
import java.nio.*;

public class ProxyTriple {
    public String host;
    public int port;
    public int weight;
    public int rangeStart;
    public int rangeEnd;

    public ProxyTriple(String host, int port, int weight, int rangeStart, int rangeEnd){
        this.host = host;
        this.port = port;
        this.weight = weight;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }
}