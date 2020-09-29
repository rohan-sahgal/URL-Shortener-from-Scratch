
import java.net.*;
import java.io.*;
import java.util.*;

public class ProxyThread extends Thread {  
    private Socket socket = null;
    private int remoteport = 8001;
    private String host = "localhost";

    public ProxyThread(Socket socket, int remoteport) {			
        super("ProxyThread");
        this.socket = socket;
        this.remoteport = remoteport;
    }
    
    //Variable declaration

    final byte[] request = new byte[1024];
    byte[] reply = new byte[4096];

    
    /**This method handles  
        input from user
        sends request to server
        gets response from server
        sends response to user **/
    
    public void run() {
        try {

            // Print a start-up message
            System.out.println("Starting proxy for " + this.host + ":" + this.remoteport
                + " on port " + 8000);
            // And start running the server
            runServer(host, this.remoteport); // never returns
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
   * runs a single-threaded proxy server on
   * the specified local port. It never returns.
   */
  public void runServer(String host, int remoteport)
      throws IOException {

    final byte[] request = new byte[1024];
    byte[] reply = new byte[4096];

    System.out.println("client connected to proxy");

    Socket server = null;
    try {

        final InputStream streamFromClient = socket.getInputStream();
        final OutputStream streamToClient = socket.getOutputStream();

        // Make a connection to the real server.
        // If we cannot connect to the server, send an error to the
        // client, disconnect, and continue waiting for connections.
        try {
          server = new Socket(host, remoteport);
          System.out.println("Connected to real server");
        } catch (IOException e) {
          PrintWriter out = new PrintWriter(streamToClient);
          out.print("Proxy server cannot connect to " + host + ":"
              + remoteport + ":\n" + e + "\n");
          out.flush();
          socket.close();
          return;
        }

        // Get server streams.
        final InputStream streamFromServer = server.getInputStream();
        final OutputStream streamToServer = server.getOutputStream();

        // a thread to read the client's requests and pass them
        // to the server. A separate thread for asynchronous.
        Thread t = new Thread() {
          public void run() {
            int bytesRead;
            try {
              while ((bytesRead = streamFromClient.read(request)) != -1) {
                streamToServer.write(request, 0, bytesRead);
                streamToServer.flush();
              }
            } catch (IOException e) {
            }

            // the client closed the connection to us, so close our
            // connection to the server.
            try {
              streamToServer.close();
            } catch (IOException e) {
            }
          }
        };

        // Start the client-to-server request thread running
        t.start();

        // Read the server's responses
        // and pass them back to the socket.
        int bytesRead;
        try {
          while ((bytesRead = streamFromServer.read(reply)) != -1) {
            streamToClient.write(reply, 0, bytesRead);
            streamToClient.flush();
          }
        } catch (IOException e) {
        }

        // The server closed its connection to us, so we close our
        // connection to our socket.
        streamToClient.close();
    } catch (IOException e) {
        System.err.println(e);
    } finally {
        try {
          if (server != null)
            server.close();
          if (socket != null)
            socket.close();
        } catch (IOException e) {
        }
    }

  }
}