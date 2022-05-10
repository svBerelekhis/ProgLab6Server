package server;

import command.Command;
import command.CommandNames;
import object.ListOfFlat;
import object.Request;


import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class ServerWorker {
    StringBuffer ans = new StringBuffer();
    String user;
    ListOfFlat listOfFlat;
    private final Socket incoming;

    public ServerWorker(Socket incoming) {
        this.incoming = incoming;
    }

    public void work(){
        boolean isWork = true;

        Word word =  Word.getWord(this);
            try (ObjectInputStream getFromClient = new ObjectInputStream(incoming.getInputStream());
                      ObjectOutputStream sendToClient = new ObjectOutputStream(incoming.getOutputStream())) {
                System.out.println("Мы на IP = " + InetAddress.getLocalHost());
                System.out.println("Ожидается подключение");
                Server.logger.info("Клиент подключился к серверу");
                Object next = getFromClient.readObject();
                while (next.getClass() == Command.class && ((Command) next).getCommandName() != CommandNames.EXIT) {
                    Command command = (Command) next;
                    System.out.println(((Command) next).getCommandName().getDescription());
                    Server.logger.info("Получена новая команда" + command.getCommandName().getDescription());
                    if (command.getCommandName() == CommandNames.AUTHORIZATION) {
                        sendToClient.writeObject(new Request(Aut(command.getUser(), command.getPass())));
                    } else if (command.getCommandName() == CommandNames.NEW_USER) {
                        sendToClient.writeObject(new Request(NewUs(command.getUser(), command.getPass())));
                    } else if (command.getCommandName() == CommandNames.CHANGE_PASS) {
                        sendToClient.writeObject(new Request(chPass(command.getPass())));
                    }
                    else {
                        word.execute(command);
                        if (this.listOfFlat != null) {
                            sendToClient.writeObject(this.listOfFlat);
                        } else {
                            sendToClient.writeObject(ans.toString());
                        }
                    }
                    ans = new StringBuffer();
                    next = getFromClient.readObject();



                }
            } catch (SocketException e){
                Server.logger.info("Пользователь отключился");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
    }

    public void addToAns(String ans) {
        this.ans.append(ans);
    }
    public void addToAns(ListOfFlat listOfFlat){
        this.listOfFlat = listOfFlat;
    }

    private String Aut(String name, String pass){
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement request = conn.createStatement()) {
            conn.setAutoCommit(false);
            ResultSet rs = request.executeQuery ("SELECT * FROM USERS WHERE name = '" + name + "';");
            String nowPass = null;
            String nowUs = null;
            while ( rs.next()) {
                nowPass = rs.getString("pass");}
            if (pass.equals(nowPass)){
                Word word = Word.getWord();
                word.setMasterName(name);
                this.user = name;
                System.out.println(this.user);
                ans.append("YES");
                return "YES";
            }else {
                ans = new StringBuffer();
                ans.append("NO");
                return "NO";
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return "NO";
        }
    }
    private String NewUs(String name, String pass){
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement request = conn.createStatement()) {
            conn.setAutoCommit(false);
            String msg = "INSERT INTO USERS (name, pass) VALUES ('" + name + "', '" + pass + "');";
            request.executeUpdate(msg);
            conn.commit();
            Word word = Word.getWord();
            word.setMasterName(name);
            this.user = name;
            return "YES";
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return "NO";
        }
    }
    private String chPass(String pass){
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement request = conn.createStatement()) {
            conn.setAutoCommit(false);
            String msg = "UPDATE USERS SET PASS = '" + pass +"' WHERE NAME= '" + this.user + "';";
            request.executeUpdate(msg);
            conn.commit();
            return "YES";
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return "NO";
        }
    }


}
