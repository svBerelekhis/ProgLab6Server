package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {
    public static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        logger.info("Сервер начал работу");
        DatabaseManager.getInstance(); //Загрузить БД
        Word.getWord(); //Загрузить коллекцию
        try (ServerSocket server = new ServerSocket(8080)) {
            System.out.print("Сервер начал слушать клиентов: " + "Порт " + server.getLocalPort() +
                    " / Адрес " + InetAddress.getLocalHost() + ".\nОжидаем подключения клиентов");
            Thread pointer = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.print(" .");
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            pointer.setDaemon(true);
            pointer.start();
            while (true) {
                Socket incoming = server.accept();
                pointer.interrupt();
                System.out.println(incoming + " подключился к серверу.");
                ServerWorker serverWorker = new ServerWorker(incoming);
                serverWorker.work();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
