
import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;

        

public class MultiThreadedProxy {
    public static void main(String[] args) {
        
        if ((args.length < 5) || ((args.length - 2) % 4 != 0)){
            throw new IllegalArgumentException("Wrong number of arguments!\nUsage: java MultiThreadedProxy [local port] [threads] [remote host] [remote port] [weight] [range]");
        }

        ProxyTripleList triples = new ProxyTripleList();

        int port_num = Integer.parseInt(args[0]);	//default port_num number
        int numWorkers = Integer.parseInt(args[1]);

        int j = 2;
        while (j < args.length){
            
            String host;
            int localport;
            int weight;
            String range;

            try {
                host = args[j++];
                localport = Integer.parseInt(args[j++]);
                weight = Integer.parseInt(args[j++]);
                range = args[j++];
                triples.add(new ProxyTriple(host, localport, weight, range));
            } catch (NumberFormatException e) {
                System.err.println("port or weight not a number");
                System.exit(-1);
            }
            
        }

        
        //Variable Declaration 
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            serverSocket = new ServerSocket(port_num);
            System.out.println("Started on: " + port_num);
            
        } catch (IOException e) {
        }

        Work w = new Work(serverSocket);

        ProxyThread [] t = new ProxyThread[numWorkers];

		for(int i = 0;i<numWorkers;i++){
			t[i] = new ProxyThread(w, triples);
			t[i].start();
		}

        System.out.println("Finished Starting Threads");
		try {
			for(int i=0;i<numWorkers;i++){
				t[i].join();
			}

		} catch (InterruptedException e) {

		}

        try {
            serverSocket.close();
        } catch (Exception e) {
            System.err.println(e);
        }

    }
}

