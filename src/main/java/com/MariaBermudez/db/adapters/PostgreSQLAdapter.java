package com.MariaBermudez.db.adapters;
import java.sql.*;

public class PostgreSQLAdapter implements IAdapter {
    @Override
    public Connection conectar(String url, String user, String pass) throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(url, user, pass);
    }
}