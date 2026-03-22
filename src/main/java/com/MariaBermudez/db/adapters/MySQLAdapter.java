package com.MariaBermudez.db.adapters;
import java.sql.*;

public class MySQLAdapter implements IAdapter {
    @Override
    public Connection conectar(String url, String user, String pass) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, pass);
    }
}