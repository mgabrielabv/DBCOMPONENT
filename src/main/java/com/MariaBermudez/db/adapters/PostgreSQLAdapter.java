package com.MariaBermudez.db.adapters;

public class PostgreSQLAdapter implements IAdapter {
    @Override
    public java.sql.Connection conectar(String url, String user, String pass) throws Exception {
        Class.forName("org.postgresql.Driver");
        return java.sql.DriverManager.getConnection(url, user, pass);
    }
}