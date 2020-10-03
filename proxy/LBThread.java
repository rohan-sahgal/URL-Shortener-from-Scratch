
import java.net.*;
import java.io.*;
import java.util.*;

import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LBThread extends Thread {  
    private Work w = null;
    private int numBuckets = 100;
    private ProxyTripleList triples = null;
    

    public LBThread(Work w, ProxyTripleList triples) {			
        super("LBThread");
        this.w = w;
        this.triples = triples;
    }

    
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
            while (true) {
              runServer(w.getWork());
            }
             // never returns
        } catch (Exception e) {
            System.err.println(e);
        }
    }

      /**
     * runs a single-threaded proxy server on
     * the specified local port. It never returns.
     */
    public void runServer(Socket socket)
        throws IOException {

      if (socket == null) return;
      byte[] reply = new byte[4096];

      System.out.println("client connected to proxy");

      Socket server = null;
      try {

          // client input streams and buffers
          final InputStream streamFromClient = socket.getInputStream();
          BufferedReader in = new BufferedReader(new InputStreamReader(streamFromClient));
          final OutputStream streamToClient = socket.getOutputStream();
          
          // logic for data distrbution to get correct server based on the short url
          String shortResource = null;
          final String  request;
          try {
              request = in.readLine();
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
                  PrintWriter out = new PrintWriter(streamToClient);
                  out.print("Proxy got Invalid request " + request + ":"
                      + ":\n");
                  out.flush();
                  socket.close();
                  return;
                }
              }
              
            }
          catch (Exception e) {
              System.err.println("Server error");
              PrintWriter out = new PrintWriter(streamToClient);
              out.print("Proxy got a Server error\n");
              out.flush();
              socket.close();
              return;
          }

          int bucketIndex = getBucket(shortResource);
          if (bucketIndex == -1) {
              System.err.println("Server error");
              PrintWriter out = new PrintWriter(streamToClient);
              out.print("Proxy got a Server error\n");
              out.flush();
              socket.close();
              return;
          }
          
          ProxyTriple currentServer = triples.getByBucket(bucketIndex);
          String host = currentServer.host;
          int remoteport = currentServer.port;

          System.out.println("Connecting to real server " + host + ":" + remoteport);

          // Make a connection to the real server.
          // If we cannot connect to the server, send an error to the
          // client, disconnect, and continue waiting for connections.
          try {
            server = new Socket(host, remoteport);
            System.out.println("Connected to real server " + host + ":" + remoteport);
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
              PrintWriter outToServer = new PrintWriter(streamToServer);
              String input_Line;
              try {
                outToServer.println(request);
                outToServer.flush();
                streamToServer.flush();
                while ((input_Line = in.readLine()) != null) {
                  outToServer.println(input_Line);
                  outToServer.flush();
                  streamToServer.flush();
                }
              } catch (IOException e) {
              }

              // the client closed the connection to us, so close our
              // connection to the server.
              try {
                outToServer.close();
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
      return Math.abs(hash % numBuckets);
    }

  }