package nogrunt.dbconn;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DbConnectionPool {

    private static BasicDataSource dataSource = new BasicDataSource();

    static {
        // Database configuration
        dataSource.setUrl("jdbc:mysql://localhost:3306/yourDatabase");
        dataSource.setUsername("yourUsername");
        dataSource.setPassword("yourPassword");

        // Pool configuration
        dataSource.setInitialSize(5); // Initial number of connections
        dataSource.setMaxTotal(10); // Maximum number of connections
        dataSource.setMaxIdle(5); // Maximum number of idle connections
        dataSource.setMinIdle(2); // Minimum number of idle connections
        // ... add other configurations as needed
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Example usage
    public static void main(String[] args) {
        try (Connection connection = getConnection()) {
            // Use the connection here
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
