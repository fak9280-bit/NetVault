package com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private static int PORT= 8080;
    static Map<String, String> map = new ConcurrentHashMap<>();
    private static final ExecutorService threadPool =Executors.newFixedThreadPool(20);
    public static void socketServer() throws IOException {
        InetSocketAddress address =new InetSocketAddress("0.0.0.0",PORT);
        System.out.println("Загрузка базы");
        map=Converttojson.loadtojson();
        System.out.println("База загружена");
        try (ServerSocket serverSocket =new ServerSocket()) {
            serverSocket.bind(address);
            System.out.println("Сервер запущен и слушает порт  " + PORT + "....");
            while (true) {
                System.out.println("Ожидание подключения...");

                Socket clientSocket = serverSocket.accept();

                System.out.println("Подключен :" + clientSocket.getInetAddress());

                threadPool.execute(()-> {
                    try {
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private static void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Команда HELP для справки");
         try(Socket socket =clientSocket) {
            while (true) {

                String message = in.readLine();

                if (message == null || message.trim().isEmpty()) {
                    System.out.print("Клиент отправил пустое сообщение ");
                    return;
                }
                System.out.println("Получена команда " + message);
                String[] parts = message.trim().split("\\s+");

                String command = parts[0].toUpperCase();

                switch (command) {
                    case "SET" -> {
                        if (parts.length >= 3) {
                            map.put(parts[1], parts[2]);
                            out.println("OK");
                            Converttojson.savetosjon(map);
                        } else {
                            out.println("Не верный формат");
                        }
                    }
                    case "GET" -> {
                        if (parts.length >= 2) {
                            out.println(map.get(parts[1]));
                        } else {
                            out.println("Не верный формат");
                        }
                    }
                    case "DEL" -> {
                        if (parts.length >= 2) {
                            out.println("Удалено " + map.get(parts[1]));
                            map.remove(parts[1]);
                        } else {
                            out.println("Не верный формат");
                        }
                    }
                    case "EXIT" -> {
                        out.println("Соединение закрыто");
                        clientSocket.close();
                        return;
                    }
                    case "HELP" -> {
                        out.println("SET key value \n GET key \n DEL key \n EXIT \n HELP");
                        return;
                    }
                }
            }
         }
            catch (Exception e) {
             e.printStackTrace();

         }

    }

}
