package server;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

public final class DatabaseManager {

    private String DB_URL;
    private String USER;
    private String PASSWORD;
    private static volatile DatabaseManager instance;

    {
        try {
            Class.forName("org.postgresql.Driver");
            Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/DBForTestLab7",
                            "postgres", "bA5aspak");
            this.DB_URL = "jdbc:postgresql://localhost:5432/lab6DB4";
            this.USER = "postgres";
            this.PASSWORD = "bA5aspak";
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Предоставляет доступ к БД. БД должна быть одна на приложение, поэтому реализован singleton шаблон.
     */
    public static DatabaseManager getInstance() {
        DatabaseManager instance2 = instance;
        if (instance2 == null) {
            synchronized (DatabaseManager.class) {
                instance2 = instance;
                if (instance2 == null) instance = instance2 = new DatabaseManager();
            }
        }
        return instance;
    }

    /**
     * Инициализирует БД и осуществляет пробное подключение к ней.
     */
    private DatabaseManager() {
        try (Connection testConnection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             ResultSet testRequest = testConnection.createStatement().executeQuery("SELECT version()")) {
            System.out.println("Идёт установка связи с БД.");
            while (testRequest.next())
                System.out.println("Связь с БД установлена." + " Версия: " + testRequest.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Возвращает новое соединение с БД.
     * @return объект {@link Connection} для связи.
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public String getDB_URL() {
        return DB_URL;
    }

    public String getUSER() {
        return USER;
    }

    public String getPASSWORD() {
        return PASSWORD;
    }

    @Deprecated
    private String chooseDBProperties() {
        if (System.getProperty("os.name").equals("SunOS")) return "helios_db.properties";
        else return "local_db.properties";
    }
}
