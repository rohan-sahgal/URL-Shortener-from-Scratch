
import java.net.*;
import java.io.*;
import java.util.*;

import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyThread extends Thread {  
    private Socket socket = null;
    private int num_hosts = 0;
    private ArrayList<ProxyTriple> triples = null;
    

    public ProxyThread(Socket socket, ArrayList<ProxyTriple> triples) {			
        super("ProxyThread");
        this.socket = socket;
        this.num_hosts = triples.size();
        this.triples = triples;
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
            System.out.println("Starting proxy for ...");
            // Print a start-up message
            // System.out.println("Starting proxy for " + this.host + ":" + this.remoteport
            //     + " on port " + 8000);
            // And start running the server
            runServer(); // never returns
        } catch (Exception e) {
            System.err.println(e);
        }
    }

      /**
     * runs a single-threaded proxy server on
     * the specified local port. It never returns.
     */
    public void runServer()
        throws IOException {

      final byte[] request = new byte[1024];
      byte[] reply = new byte[4096];

      System.out.println("client connected to proxy");

      Socket server = null;
      try {

          final InputStream streamFromClient = socket.getInputStream();
          final OutputStream streamToClient = socket.getOutputStream();

          int serverIndex = 0; //parseAndGetBucket(streamFromClient);
          ProxyTriple currentServer = triples.get(serverIndex);

          String host = currentServer.host;
          int remoteport = currentServer.port;

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

    public int getBucket(String shortResource) {
      if (shortResource == null) return -1;

      int hash = shortResource.hashCode();
      return Math.abs(hash % num_hosts);
    }

    public int parseAndGetBucket(InputStream clientInput){
      String shortResource = null;
      BufferedReader in = null;
      try {
        in = new BufferedReader(new InputStreamReader(clientInput));
        String request = in.readLine();
        Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
        Matcher mput = pput.matcher(request);

        if(mput.matches()){
          shortResource = mput.group(1);
        } 
        else {
          Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)\\s+(\\S+)$");
          Matcher mget = pget.matcher(request);
          if(mget.matches()) {
              String method=mget.group(1);
              shortResource=mget.group(2);
          }
          else {
            System.err.println("Invalid request");
          }
        }
        
      }

      catch (Exception e) {
        System.err.println("Server error");
        return -1;
      }

      finally {
        try {
          in.close();
        } catch (Exception e) {
          System.err.println("Error closing stream : " + e.getMessage());
        } 
      }
      return getBucket(shortResource);
    }
  }