package ca.uwo.csd.ai.nlp.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DBUtil {

    private Connection con;
    private Statement stmt;
    private String hostname;
    private String port;
    private String user;
    private String password;
    
    public DBUtil(String hostname, String port, String user, String password) {
        this.hostname = hostname;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public DBUtil() {
        this("localhost", "3306", "root", "");
    }

    public boolean connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/ppi", user, password);
            stmt = con.createStatement();
            return true;
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return false;
    }

    public boolean connect(String dbName) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName, user, password);
            stmt = con.createStatement();
            return true;
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return false;
    }

    public ResultSet execQuery(String sql) {
        try {
            return stmt.executeQuery(sql);
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return null;
    }

    public ResultSet execQuery(Statement stmt, String sql) {
        try {
            return stmt.executeQuery(sql);
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return null;
    }

    public void execUpdate(String sql) {
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }

    public Statement createStatement() {
        try {
            return con.createStatement();
        } catch (SQLException ex) {
            Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public Connection getConnection() {
        return con;
    }

    public void closeConnection() {
        try {
            con.commit();
            con.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void main(String args[]) throws SQLException {
        DBUtil dbUtil = new DBUtil();
        if (dbUtil.connect("ppi")) {
            ResultSet rs = dbUtil.execQuery("select * from interaction limit 6");
            while (rs.next()) {
                System.out.println(rs.getString(1) + ", " + rs.getString(2) + ", " + rs.getString(3) + ", " + rs.getString(4));
            }
        }

    }
}
