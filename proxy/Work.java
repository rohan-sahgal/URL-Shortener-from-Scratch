
import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;

        

public class Work {
    private ServerSocket serverSocket = null;
    
    public Work(ServerSocket serverSocket) {			
        this.serverSocket = serverSocket;
    }

    synchronized Socket getWork() throws Exception {
        return serverSocket.accept();
    }
}

