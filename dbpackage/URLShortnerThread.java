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

public class URLShortnerThread extends Thread {
	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "../index.html";
	static final String FILE_NOT_FOUND = "../404.html";
	static final String METHOD_NOT_SUPPORTED = "../not_supported.html";
	static final String REDIRECT_RECORDED = "../redirect_recorded.html";
	static final String REDIRECT = "../redirect.html";
	static final String NOT_FOUND = "../notfound.html";
	static final String DATABASE = "../database.txt";

	// verbose mode
	private boolean verbose = true;
	private URLShortnerSQL sql = null;
	private ReadWriteLock readWriteLock = null;
	private Socket connect = null;
	private int numBuckets = 100;

	public URLShortnerThread(Socket connect, URLShortnerSQL sql, ReadWriteLock readWriteLock, boolean verbose) {			
    super("URLShortnerThread");
    this.connect = connect;
    this.sql =  sql;
		this.readWriteLock = readWriteLock;
		this.verbose = verbose;
  }

	public void run() {
    try {
			handle();
    } catch (Exception e) {
      System.err.println(e);
    }
  }

	public void handle() {
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;

		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			String input = in.readLine();
			
			if(verbose)System.out.println("first line: "+input);
			Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
			Matcher mput = pput.matcher(input);
			if(mput.matches()){
				String shortResource=mput.group(1);
				String longResource=mput.group(2);
				String httpVersion=mput.group(3);
				int hash = getBucket(shortResource);
				
				readWriteLock.writeLock().lock();
				sql.insert(shortResource, longResource);
				readWriteLock.writeLock().unlock();

				File file = new File(WEB_ROOT, REDIRECT_RECORDED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
        		if(verbose)System.out.println("fileLength: "+fileLength);
					
				out.println("HTTP/1.1 200 OK");
				out.println("Server: Java HTTP Server/Shortner : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println(); 
				out.flush(); 

				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			} else {
				Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)\\s+(\\S+)$");
				Matcher mget = pget.matcher(input);
				if(mget.matches()){
					String method=mget.group(1);
					String shortResource=mget.group(2);
					String httpVersion=mget.group(3);
					int hash = getBucket(shortResource);

					readWriteLock.readLock().lock();
					String longResource = sql.select(shortResource);
					readWriteLock.readLock().unlock();

					if (longResource != null) {
						File file = new File(WEB_ROOT, REDIRECT);
						int fileLength = (int) file.length();
						String contentMimeType = "text/html";
	
						//read content to return to client
						byte[] fileData = readFileData(file, fileLength);
						
						// out.println("HTTP/1.1 301 Moved Permanently");
						out.println("HTTP/1.1 307 Temporary Redirect");
						out.println("Location: "+longResource);
						out.println("Server: Java HTTP Server/Shortner : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + contentMimeType);
						out.println("Content-length: " + fileLength);
						out.println(); 
						out.flush(); 
	
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					} else {
						File file = new File(WEB_ROOT, FILE_NOT_FOUND);
						int fileLength = (int) file.length();
						String content = "text/html";
						byte[] fileData = readFileData(file, fileLength);
						
						out.println("HTTP/1.1 404 File Not Found");
						out.println("Server: Java HTTP Server/Shortner : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + content);
						out.println("Content-length: " + fileLength);
						out.println(); 
						out.flush(); 
						
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					}
				}
			}
		} catch (Exception e) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.err.println("Server error");
		} finally {
			try {
				in.close();
				out.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}

	private int getBucket(String shortResource) {
      if (shortResource == null) return -1;

      int hash = shortResource.hashCode();
      return Math.abs(hash % numBuckets);
    }
}
