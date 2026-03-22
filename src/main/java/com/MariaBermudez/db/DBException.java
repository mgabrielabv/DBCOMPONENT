package com.MariaBermudez.db;
import java.sql.SQLException;

public class DBException extends Exception {
    public enum Category { CONNECTION, AUTH, SYNTAX, CONSTRAINT, UNKNOWN }
    private final Category category;

    public DBException(Category category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public static DBException fromSQLException(SQLException e) {
        String state = e.getSQLState();
        Category cat = (state == null || state.length() < 2) ? Category.UNKNOWN : switch (state.substring(0, 2)) {
            case "08" -> Category.CONNECTION;
            case "28" -> Category.AUTH;
            case "42" -> Category.SYNTAX;
            case "23" -> Category.CONSTRAINT;
            default -> Category.UNKNOWN;
        };
        return new DBException(cat, e.getMessage(), e);
    }
    public Category getCategory() { return category; }
}