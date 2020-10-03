import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;

public class ProxyTripleList {
    private ArrayList<ProxyTriple> triples = new ArrayList<ProxyTriple>();
    private int roundRobinPtr = 0;
    private int buckets = 100;
    Integer[] dist = new Integer[100];
    // start end pair
    // Pair<Integer, Integer> start_end = 

    synchronized void add(ProxyTriple proxyTriple) {
        triples.add(proxyTriple);
        updateDist();
    }

    private void updateDist() {
       int size = triples.size();
       int part = (int) Math.floor(buckets / size);

       for(int i = 0;i < size;i++){
           int start = i * part;
           int end;
           if (i == size -1) {
               end = buckets;
           } else {
               end = (i + 1) * part;
           }

           for (int j = start; j < end; j++ ){
               dist[j] =  i;
           }
	   }
    }
    
    synchronized void remove(ProxyTriple proxyTriple) {
        triples.remove(proxyTriple);
        updateDist();
    }

    synchronized ProxyTriple getByBucket(int idx) {
        return triples.get(dist[idx]);
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