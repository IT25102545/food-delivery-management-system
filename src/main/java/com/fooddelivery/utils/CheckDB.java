import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-online-food-delivery-system-online-food-delivery-system.h.aivencloud.com:15420/defaultdb?sslMode=REQUIRED";
        String user = "avnadmin";
        String pass = "YOUR_AIVEN_PASSWORD_HERE";
        
        try {
            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected to Aiven!");
            
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM orders");
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnsNumber = rsmd.getColumnCount();
                System.out.println("Orders table exists. Columns:");
                for (int i = 1; i <= columnsNumber; i++) {
                    System.out.println(rsmd.getColumnName(i) + " (" + rsmd.getColumnTypeName(i) + ")");
                }
            } catch (SQLException e) {
                System.out.println("SQL ERROR: " + e.getMessage());
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
