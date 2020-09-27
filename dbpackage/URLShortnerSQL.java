import java.sql.*;

public class URLShortnerSQL { 

	private static final String URL = "jdbc:sqlite:/student/hameedso/csc409/a1/repo_a1group77/db/url.db";
	private static Connection connection;

	private static final String REPLACE_STATEMENT = "REPLACE INTO URLS (SHORT,LONG) VALUES(?,?)";
	private static final String SELECT_ALL = "SELECT * FROM URLS ";
	private static final String SELECT_BY_SHORT = "SELECT * FROM URLS WHERE SHORT=?";

	public static void main(String[] args) {
		URLShortnerSQL sql = new URLShortnerSQL();
		sql.insertOrReplace("rohan", "sim");
		sql.printAll();
		String longURL = sql.findByShortURL("bby");
		System.out.println(longURL);
		longURL = sql.findByShortURL("rohan");
		System.out.println(longURL);
	}

	/**
	* Connects the program to the main SQL database.
	*
	* @return a connection to the database
	*/
	public static Connection getConnection() {

		if (URLShortnerSQL.connection == null) {
			try {
				Class.forName("org.sqlite.JDBC");
				URLShortnerSQL.connection = DriverManager.getConnection(URLShortnerSQL.URL);
				System.out.println("Opened database successfully");
			} catch (Exception e) {
				System.err.println( e.getClass().getName() + ": " + e.getMessage() );
				System.exit(0);
			}
		}
		return URLShortnerSQL.connection;
	}

	public static void insertOrReplace(String shortURL,String longURL) {
		Connection c = getConnection();

		try {
			Class.forName("org.sqlite.JDBC");
			PreparedStatement preparedStatement = c.prepareStatement(REPLACE_STATEMENT);
			preparedStatement.setString(1, shortURL);
    		preparedStatement.setString(2, longURL);
			preparedStatement.executeUpdate();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
		System.out.println("Records created successfully");
	}

	public static ResultSet selectAll() throws SQLException {
		Connection c = getConnection();
    	Statement statement = c.createStatement();
    	return statement.executeQuery(SELECT_ALL);
  	}
	
	public static void printAll() {
		try {
			Class.forName("org.sqlite.JDBC");

			ResultSet rs = selectAll();
			
			while ( rs.next() ) {
				int id = rs.getInt("id");
				String shortURL = rs.getString("short");
				String longURL = rs.getString("long");
				
				System.out.println( "ID = " + id );
				System.out.println( "SHORT = " + shortURL );
				System.out.println( "LONG = " + longURL );
				System.out.println();
			}
			rs.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		}
		
		System.out.println("Operation done successfully");
	}

	public static ResultSet selectByShortURL(String shortURL) throws SQLException {
		Connection c = getConnection();
		PreparedStatement preparedStatement = c.prepareStatement(SELECT_BY_SHORT);
		preparedStatement.setString(1, shortURL);
		return preparedStatement.executeQuery();
	}

	public static String findByShortURL(String shortURL){
		String longURL = null;
		try {
			ResultSet resultSet = selectByShortURL(shortURL);
			if (resultSet.next()) {
				longURL = resultSet.getString("long");
			}
		} catch (Exception e) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		} 
		return longURL;
	}

}
