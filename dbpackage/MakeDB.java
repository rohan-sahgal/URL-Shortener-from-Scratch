import java.sql.*;

public class MakeDB {

	/**
	* Connects the program to the main SQL database. 
	*
	* @return a connection to the database
	*/
	public static Connection getConnection(String url) {
		Connection c = null;
      	try {
         	Class.forName("org.sqlite.JDBC");
         	c = DriverManager.getConnection(url);
      	} catch ( Exception e ) {
         	System.err.println( e.getClass().getName() + ": " + e.getMessage() );
         	System.exit(0);
      	}
      	System.out.println("Opened database successfully");
		return c;
	}

    public static void createNewTable(Connection c) {

		Statement stmt = null;
		
		try {
			Class.forName("org.sqlite.JDBC");

			stmt = c.createStatement();
			String sql = "CREATE TABLE URLS " +
						 "(ID       INTEGER PRIMARY KEY AUTOINCREMENT," +
						 " SHORT    TEXT    NOT NULL UNIQUE, " + 
						 " LONG     TEXT    NOT NULL," +
						 " HASH     INTEGER NOT NULL)"; 

			stmt.executeUpdate(sql);
			sql = "CREATE INDEX HASH_IDX ON URLS" +
						 "(HASH)"; 
			stmt.executeUpdate(sql);
			stmt.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
		System.out.println("Table created successfully");
    }
	
	public static void testInsert(Connection c) {
		Statement stmt = null;

		try {
			Class.forName("org.sqlite.JDBC");
			stmt = c.createStatement();
			String sql = "REPLACE INTO URLS (SHORT,LONG,HASH) " +
						 "VALUES ('Paul', 'California', 1);"; 
			stmt.executeUpdate(sql);
			stmt.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
		System.out.println("Records created successfully");
	}
	

	public static void testSelect(Connection c) {
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT * FROM URLS;" );
			
			while ( rs.next() ) {
				int id = rs.getInt("id");
				String short_var = rs.getString("short");
				String long_var = rs.getString("long");
				int hash = rs.getInt("hash");
				
				System.out.println( "ID = " + id );
				System.out.println( "SHORT = " + short_var );
				System.out.println( "LONG = " + long_var );
				System.out.println( "HASH = " + hash );
				System.out.println();
			}
			rs.close();
			stmt.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
		
		System.out.println("Operation done successfully");
	}

    public static void main(String[] args) {

		if ((args.length != 2)){
            throw new IllegalArgumentException("Wrong number of arguments!\nUsage: java MakeDB fullPath fileName");
			// "jdbc:sqlite:/student/hameedso/csc409/a1/repo_a1group77/db/"
        }
		String fileName = args[0];
		String url = args[1] + fileName;
		System.out.println( url);
		Connection c = getConnection(url);
		createNewTable(c);
		testInsert(c);
		testSelect(c);
		try {
			c.close();
			System.out.println("connection closed");
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}

    }
}