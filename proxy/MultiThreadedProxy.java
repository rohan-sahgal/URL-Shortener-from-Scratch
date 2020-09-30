
import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;

        

public class MultiThreadedProxy {
    public static void main(String[] args) {
        
        if ((args.length == 0) || (args.length % 3 != 0)){
            throw new IllegalArgumentException("Wrong number of arguments!\nUsage: java proxy [remote port] [local port]");
        }



        ArrayList<ProxyTriple> triples = new ArrayList<ProxyTriple>();

        int j = 0;
        while (j < args.length){
            
            String host;
            int localport;
            int weight;

            try {
                host = args[j++];
                localport = Integer.parseInt(args[j++]);
                weight = Integer.parseInt(args[j++]);
                triples.add(new ProxyTriple(host, localport, weight));
            } catch (NumberFormatException e) {
                System.err.println("port or weight not a number");
                System.exit(-1);
            }
            
        }

        
        //Variable Declaration 
        ServerSocket serverSocket = null;
        boolean listening = true;

        int port_num = 8000;	//default port_num number
        // try {
        //     port_num = Integer.parseInt(args[0]);
        // } catch (Exception e) {

        // }

        try {
            serverSocket = new ServerSocket(port_num);
            System.out.println("Started on: " + port_num);
        } catch (IOException e) {
        }

        int fwd_port = 8001;
        try {
            while (listening) {
                new ProxyThread(serverSocket.accept(), triples).start();
            }
            serverSocket.close();
        } catch (Exception e) {
            System.err.println(e);
        }

        // hash short url
        // 

    }
}

