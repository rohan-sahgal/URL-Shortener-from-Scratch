
import java.net.*;
import java.io.*;
import java.util.*;

import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyThread extends Thread {  
    private Work w = null;
    private int num_hosts = 0;
    private ProxyTripleList triples = null;
    

    public ProxyThread(Work w, ProxyTripleList triples) {			
      super("ProxyThread");
      this.w = w;
      this.num_hosts = triples.size();
      this.triples = triples;
    }

    
    /**This method handles  
        input from user
        sends request to server
        gets response from server
        sends response to user **/
    
    public void run() {
      try {
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
      final byte[] request = new byte[1024];

      System.out.println("Client connected to proxy");

      Socket server = null;
      try {
        // client input streams and buffers
        final InputStream streamFromClient = socket.getInputStream();
        final OutputStream streamToClient = socket.getOutputStream();
        
        int serverIndex = triples.getServerIdx();
        ProxyTriple currentServer = triples.get(serverIndex);
        String host = currentServer.host;
        int remoteport = currentServer.port;

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
              try {
                while ((bytesRead = streamFromClient.read(request)) != -1) {
                  streamToServer.write(request, 0, bytesRead);
                  streamToServer.flush();
                }
              } catch (IOException e) {
                System.err.println("thread reading proxy : " + e.getMessage());
              }

              // the client closed the connection to us, so close our
              // connection to the server.
              try {
                streamToServer.close();
              } catch (IOException e) {
                System.err.println("thread : " + e.getMessage());
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
            System.err.println("halfway : " + e.getMessage());
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