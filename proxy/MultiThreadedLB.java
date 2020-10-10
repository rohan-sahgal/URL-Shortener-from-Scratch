
import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;

        

public class MultiThreadedLB {
    public static void main(String[] args) {
        
        if ((args.length < 7) || ((args.length - 2) % 5 != 0)){
            throw new IllegalArgumentException("Wrong number of arguments!\nUsage: java MultiThreadedLB [local port] [threads] [remote host] [remote port] [weight] [range_start] [range_end]");
        }

        ProxyTripleList triples = new ProxyTripleList();

        int port_num = Integer.parseInt(args[0]);	//default port_num number
        int numWorkers = Integer.parseInt(args[1]);

        int j = 2;
        while (j < args.length){
            
            String host;
            int localport;
            int weight;
            int rangeStart;
            int rangeEnd;

            try {
                host = args[j++];
                localport = Integer.parseInt(args[j++]);
                weight = Integer.parseInt(args[j++]);
                rangeStart = Integer.parseInt(args[j++]);
                rangeEnd = Integer.parseInt(args[j++]);
                triples.add(new ProxyTriple(host, localport, weight, rangeStart, rangeEnd));
            } catch (NumberFormatException e) {
                System.err.println("port or weight not a number");
                System.exit(-1);
            }
            
        }

        
        //Variable Declaration 
        ServerSocket serverSocket = null;
        boolean listening = true;


        // try {
        //     port_num = Integer.parseInt(args[0]);
        // } catch (Exception e) {

        // }

        try {
            serverSocket = new ServerSocket(port_num);
            System.out.println("Started on: " + port_num);
            
        } catch (IOException e) {
        }

        Work w = new Work(serverSocket);

        LBThread [] t = new LBThread[numWorkers];

		for(int i = 0;i<numWorkers;i++){
			t[i] = new LBThread(w, triples);
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

