import java.sql.*;

public class URLShortnerSQL { 

	private String URL = null;

	public static void main(String[] args) {
		if ((args.length != 2)){
            throw new IllegalArgumentException("Wrong number of arguments!\nUsage: java URLShortner fullPath fileName");
    }
		// String fileName = args[0];
		// String url = args[1] + fileName;
		// URLShortnerSQL sql = new URLShortnerSQL(url);
		// sql.insertOrReplace("rohan", "sim", 1);
		// sql.printAll();
		// String longURL = sql.findByShortURL("bby");
		// System.out.println(longURL);
		// longURL = sql.findByShortURL("rohan");
		// System.out.println(longURL);
	}

	public URLShortnerSQL(String URL) {
		this.URL = URL;
	}

	public void setupDB() {
		Statement stmt = null;
		Connection connect = null;
		try {
			connect = DriverManager.getConnection(this.URL);
			System.out.println("Connection to SQLite has been established.");
			stmt = connect.createStatement();
			String sql = "CREATE TABLE IF NOT EXISTS URLS " +
										"(SHORT TEXT PRIMARY KEY NOT NULL," +
										" LONG TEXT NOT NULL);"; 
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (connect != null) {
					connect.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

	public void insert(String shortURL, String longURL) {
		Statement stmt = null;
		Connection connect = null;
		try {
			connect = DriverManager.getConnection(this.URL);
			System.out.println("Connection to SQLite has been established.");
			stmt = connect.createStatement();
			String sql = String.format("INSERT INTO URLS (SHORT, LONG) VALUES (\"%s\", \"%s\") ON CONFLICT(SHORT) DO UPDATE SET LONG=\"%s\";", shortURL, longURL, longURL);
			stmt.executeUpdate(sql);
			System.out.println("Records created successfully");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (connect != null) {
					connect.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

	public ResultSet selectAll() throws SQLException {
		Statement stmt = null;
		Connection connect = null;
		ResultSet rs = null;
		try {
			connect = DriverManager.getConnection(this.URL);
			System.out.println("Connection to SQLite has been established.");
			stmt = connect.createStatement();
			rs = stmt.executeQuery("SELECT * FROM URLS;");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (connect != null) {
					connect.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getClass().getName() + ": " + e.getMessage());
			}
		}
		return rs;
  }
	
	public void printAll() {
		try {
			ResultSet rs = selectAll();
			
			while (rs != null && rs.next()) {
				String shortURL = rs.getString("short");
				String longURL = rs.getString("long");
				
				System.out.println( "SHORT = " + shortURL );
				System.out.println( "LONG = " + longURL );
				System.out.println();
			}
			rs.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
		System.out.println("Operation done successfully");
	}

	public String select(String shortURL) {
		String longURL = null;
		Statement stmt = null;
		Connection connect = null;
		try {
			connect = DriverManager.getConnection(this.URL);
			System.out.println("Connection to SQLite has been established.");
			stmt = connect.createStatement();
			ResultSet rs = stmt.executeQuery(String.format("SELECT LONG FROM URLS WHERE SHORT=\"%s\";", shortURL));
			while (rs.next()) {
				longURL = rs.getString("LONG");
			}
			rs.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (connect != null) {
					connect.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getClass().getName() + ": " + e.getMessage());
			}
		}
		System.out.println("Record selected successfully");
		return longURL;
	}

}
