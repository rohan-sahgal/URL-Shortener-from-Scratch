
import java.net.*;
import java.io.*;
import java.util.*;

import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException; 

public class LBThread extends Thread {  
    private Work w = null;
    private ProxyTripleList triples = null;
    private ProxyTriple backup = null;
    private ProxyTriple currentServer = null;
    

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
      Socket serverBackup = null;
      try {
          boolean isPut = false;
          // client input streams and buffers
          InputStream streamFromClient = socket.getInputStream();
          BufferedReader in = new BufferedReader(new InputStreamReader(streamFromClient));
          OutputStream streamToClient = socket.getOutputStream();
          
          // logic for data distrbution to get correct server based on the short url
          String shortResource = null;
          String request;
          try {
            request = in.readLine();
            Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
            Matcher mput = pput.matcher(request);

            if(mput.matches()){
              shortResource = mput.group(1);
              isPut = true;
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

          getServers(shortResource);
          if (currentServer == null && backup == null) {
            System.err.println("Server error");
            PrintWriter out = new PrintWriter(streamToClient);
            out.print("Proxy got a Server error\n");
            out.flush();
            socket.close();
            return;
          }
          
          String host = currentServer.host;
          int remoteport = currentServer.port;

          System.out.println("Connecting to real server " + host + ":" + remoteport);

          // Make a connection to the real server.
          // If we cannot connect to the server, send an error to the
          // client, disconnect, and continue waiting for connections.
          try {
            server = new Socket(host, remoteport);
            serverBackup = server;
            System.out.println("Connected to real server " + host + ":" + remoteport);
            if (isPut) {
              try {
                serverBackup = new Socket(backup.host, backup.port);
                System.out.println("Connected to back server " + backup.host + ":" + backup.port);
              } catch (IOException e) {}
            }
          } catch (IOException e) {
            System.out.println("Proxy server cannot connect to " + host + ":"
              + remoteport + ":\n" + e + "\n");
            try {
              server = new Socket(backup.host, backup.port);
              serverBackup = server;
              System.out.println("Connected to back server " + backup.host + ":" + backup.port);
            } catch (IOException ex) {
              PrintWriter out = new PrintWriter(streamToClient);
              out.print("Proxy server cannot connect to " + host + ":"
                  + remoteport + ":\n" + ex + "\n");
              out.flush();
              socket.close();
              return;
            }
          }

          // Get server streams.
          InputStream streamFromServer = server.getInputStream();
          OutputStream streamToServer = server.getOutputStream();

          // a thread to read the client's requests and pass them
          // to the server. A separate thread for asynchronous.
          Thread t = new Thread() {
            public void run() {
              int bytesRead;
              BufferedWriter outToServer = new BufferedWriter(new OutputStreamWriter(streamToServer));
              String input_Line;
              try {
                outToServer.write(request);
                outToServer.write("\n");
                while ((input_Line = in.readLine()) != null) {
                  outToServer.write(input_Line);
                  outToServer.write("\n");
                  if (input_Line.equals("")) {
                    outToServer.flush();
                  }
                }
              } catch (IOException e) {
                 System.err.println("thread reading lb: " + e.getMessage());
              }

              // the client closed the connection to us, so close our
              // connection to the server.
              try {
                outToServer.close();
              } catch (IOException e) {
                System.err.println("Thread : " + e.getMessage());
              }
            }
          };

          // Start the client-to-server request thread running
          t.start();

          final InputStream streamFromServerBackup = serverBackup.getInputStream();
          final BufferedWriter streamToServerBackup = new BufferedWriter(new OutputStreamWriter(serverBackup.getOutputStream()));
          if (isPut && serverBackup != server) {
            Thread tBackup = new Thread() {
              public void run() {
                int bytesRead;
                try {
                  streamToServerBackup.write(request);
                  streamToServerBackup.write("\n");
                  streamToServerBackup.flush();
                } catch (IOException e) {
                }
                try {
                  streamToServerBackup.close();
                } catch (IOException e) {
                }
              }
            };
            tBackup.start();
          }

          // Read the server's responses
          // and pass them back to the socket.
          int bytesRead;
          try {
            while ((bytesRead = streamFromServer.read(reply)) != -1) {
              System.out.println("Number or byte: " + bytesRead);
              try {
                streamToClient.write(reply, 0, bytesRead);
                streamToClient.flush();
              } catch (IOException e) {
                System.err.println("other halfway : " + e.getMessage());
                e.printStackTrace();
             } 
            }
          } catch (IOException e) {
            System.err.println("halfway : " + e.getMessage());
            e.printStackTrace();
          }

          if (isPut && serverBackup != server) {
            while (streamFromServerBackup.read(reply) != -1) {
              continue;
            }
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
            if (serverBackup != null)
              serverBackup.close();
            if (socket != null)
              socket.close();
          } catch (IOException e) {
          }
      }

    }

    public void getServers(String shortResource) {
      try {
        // MD5 hashing the short
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(shortResource.getBytes());
        BigInteger number = new BigInteger(1, messageDigest);
        int hash = number.and(new BigInteger("255")).intValue();
        for (int i = 0; i < this.triples.size(); i++) {
          ProxyTriple temp = this.triples.get(i);
          if (temp.rangeStart == 0 && temp.rangeEnd == 255) {
            backup = temp;
            continue;
          }
          if (hash >= temp.rangeStart && hash <= temp.rangeEnd) {
            currentServer = temp;
            break;
          }
        }
      } catch (NoSuchAlgorithmException e) { 
        System.out.println(e.getMessage());
      }
    }

  }
  