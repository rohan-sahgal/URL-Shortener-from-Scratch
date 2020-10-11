import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;

public class ProxyTripleList {
    private ArrayList<ProxyTriple> triples = new ArrayList<ProxyTriple>();
    private int roundRobinPtr = 0;

    synchronized void add(ProxyTriple proxyTriple) {
        triples.add(proxyTriple);
    }
    
    synchronized void remove(ProxyTriple proxyTriple) {
        triples.remove(proxyTriple);
    }

    synchronized ProxyTriple get(int idx) {
        return triples.get(idx);
    }

    synchronized int getServerIdx() {
        int temp = roundRobinPtr;
        if (roundRobinPtr == triples.size() -1) {
            roundRobinPtr = 0;
        } else {
            roundRobinPtr++;
        }
        return temp;
    }

    synchronized int size() {
        return triples.size();
    }

}