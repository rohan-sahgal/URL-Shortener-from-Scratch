
import java.net.*;
import java.io.*;
import java.nio.*;

        

public class MultiThreadedProxy {
    public static void main(String[] args) {
        
        
        //Variable Declaration 
        ServerSocket serverSocket = null;
        boolean listening = true;

        int port_num = 8000;	//default port_num number
        try {
            port_num = Integer.parseInt(args[0]);
        } catch (Exception e) {
            
        }

        try {
            serverSocket = new ServerSocket(port_num);
            System.out.println("Started on: " + port_num);
        } catch (IOException e) {
        }

        int fwd_port = 8001;
        try {
            while (listening) {
                if (fwd_port == 8001) {
                    new ProxyThread(serverSocket.accept(), fwd_port).start();
                    fwd_port = 8002;
                } else {
                    new ProxyThread(serverSocket.accept(), fwd_port).start();
                    fwd_port = 8001;
                }
            }
            serverSocket.close();
        } catch (Exception e) {
            System.err.println(e);
        }

        

    }
}