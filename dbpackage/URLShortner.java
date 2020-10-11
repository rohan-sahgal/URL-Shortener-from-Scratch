import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;

public class URLShortner {
	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "../index.html";
	static final String FILE_NOT_FOUND = "../404.html";
	static final String METHOD_NOT_SUPPORTED = "../not_supported.html";
	static final String REDIRECT_RECORDED = "../redirect_recorded.html";
	static final String REDIRECT = "../redirect.html";
	static final String NOT_FOUND = "../notfound.html";
	static final String DATABASE = "../database.txt";
	
	// verbose mode
	static final boolean verbose = true;

	public static void main(String[] args) {

		if ((args.length != 4)){
      throw new IllegalArgumentException("Wrong number of arguments!\nUsage: java URLShortner port cacheSize fileName fullPath");
    }
		String fileName = args[2];
		String url = args[3] + fileName;
		// port to listen connection
		int PORT = Integer.parseInt(args[0]);
		int CACHE_SIZE = Integer.parseInt(args[1]);

		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			URLShortnerSQL sql = new URLShortnerSQL(url);
			sql.setupDB();

			URLCache cache = new URLCache(CACHE_SIZE);

			ReadWriteLock readWriteLockDB = new ReentrantReadWriteLock();
			ReadWriteLock readWriteLockCache = new ReentrantReadWriteLock();

			// we listen until user halts server execution
			while (true) {
				if (verbose) { System.out.println("Connecton opened. (" + new Date() + ")"); }
				new URLShortnerThread(serverConnect.accept(), sql, cache, readWriteLockDB, readWriteLockCache, true).start();
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

}
