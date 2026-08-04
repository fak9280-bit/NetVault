package com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import static com.Server.*;

public class HandClient {
    private static final Logger log = LoggerFactory.getLogger(HandClient.class);
    static void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Команда HELP для справки");
        try (Socket socket = clientSocket) {
            while (true) {

                String message = in.readLine();

                if (message == null || message.trim().isEmpty()) {
                    log.info("Клиент отправил пустое сообщение ");
                    return;
                }
                log.info("Получена команда {}" , message);
                String[] parts = message.trim().split("\\s+");

                String command = parts[0].toUpperCase();

                switch (command) {
                    case "SET" -> {
                        Server.totalCommands.incrementAndGet();
                        if (parts.length >= 3) {
                            map.put(parts[1],new DataRecord(parts[2],Long.MAX_VALUE ));
                            out.println("OK");
                            cacheHits.incrementAndGet();
                        } else {
                            out.println("Не верный формат");
                            cacheMisses.incrementAndGet();
                        }
                    }
                    case "GET" -> {
                        Server.totalCommands.incrementAndGet();
                        if (parts.length >= 2) {
                            DataRecord record = map.get(parts[1]);
                            if (record == null) {
                                cacheMisses.incrementAndGet();
                                out.println("(nil)");
                            } else if (System.currentTimeMillis() > record.expireAt()) {
                                map.remove(parts[1]);
                                cacheMisses.incrementAndGet();
                                out.println("(nil)");
                            } else {
                                cacheHits.incrementAndGet();
                                out.println(record.value());
                            }
                        } else {
                            out.println("Не верный формат. Используйте: GET key");
                            cacheMisses.incrementAndGet();
                        }
                    }
                    case "DEL" -> {
                        Server.totalCommands.incrementAndGet();
                        if (parts.length >= 2) {
                            DataRecord removed = map.remove(parts[1]);
                            if (removed != null) {
                                out.println("OK (удалено)");
                                cacheHits.incrementAndGet();
                            } else {
                                out.println("(nil)");
                            }
                        } else {
                            out.println("Не верный формат");
                            cacheMisses.incrementAndGet();
                        }
                    }
                    case "EXIT" -> {
                        Server.totalCommands.incrementAndGet();
                        out.println("Соединение закрыто");
                        cacheHits.incrementAndGet();
                        Server.connectedClients.decrementAndGet();
                        return;

                    }
                    case "HELP" -> {
                        Server.totalCommands.incrementAndGet();
                        out.println("=== Доступные команды ===");
                        out.println("SET key value          - Установить значение навсегда");
                        out.println("SETEX key sec value    - Установить значение с TTL (в секундах)");
                        out.println("GET key                - Получить значение");
                        out.println("DEL key                - Удалить ключ");
                        out.println("EXISTS key             - Проверить наличие ключа (вернет 1 или 0)");
                        out.println("KEYS pattern           - Найти ключи по маске (например, user*)");
                        out.println("SAVE                   - Сохранить базу на диск");
                        out.println("EXIT                   - Закрыть соединение");
                        out.println("=======================");
                        cacheHits.incrementAndGet();
                    }
                    case "SETEX" -> {
                        Server.totalCommands.incrementAndGet();
                        if (parts.length >= 4) {
                            try {
                                cacheHits.incrementAndGet();
                                long seconds = Long.parseLong(parts[2]);

                                long expireAt = System.currentTimeMillis() + (seconds * 1000);

                                map.put(parts[1], new DataRecord(parts[3], expireAt));

                                out.println("OK");

                            } catch (NumberFormatException e) {
                                out.println("ERROR: секунды должны быть числом");
                                cacheMisses.incrementAndGet();
                            }
                        } else {
                            out.println("Не верный формат. Используйте: SETEX key seconds value");
                            cacheMisses.incrementAndGet();
                        }
                    }
                    case "SAVE"->{
                        cacheHits.incrementAndGet();
                        Server.totalCommands.incrementAndGet();
                        Converttojson.saveToJson(map);
                        out.println("OK");
                    }
                    case "EXISTS"->{

                        Server.totalCommands.incrementAndGet();
                        if (parts.length>=2)
                        {
                            cacheHits.incrementAndGet();
                            DataRecord record = map.get(parts[1]);
                            if (record == null) {
                                out.println("(nil)");
                            }
                            else if(System.currentTimeMillis()> record.expireAt()){
                                map.remove(parts[1]);
                                out.println("0");
                            }
                            else {
                                out.println("1");
                            }
                        }
                        else {
                            out.println("Неверный формат EXISTS KEY");
                            cacheMisses.incrementAndGet();
                        }
                    }
                    case "KEYS"-> {
                        cacheHits.incrementAndGet();
                        Server.totalCommands.incrementAndGet();
                        if (parts.length >= 2) {
                            String pattern = parts[1];
                            List<String> foundKeys = new ArrayList<>();
                            String regex = pattern.replace("*", ".*");

                            for (String key : map.keySet()) {
                                if (key.matches(regex)) {
                                    foundKeys.add(key);
                                }
                            }
                            if (foundKeys.isEmpty()) {
                                out.println("(empty array)");
                            } else {
                                out.println(String.join(" ", foundKeys));
                            }
                        } else {
                            out.println("Не верный формат. Используйте: KEYS pattern");
                            cacheMisses.incrementAndGet();
                        }
                    }
                    case "INFO" -> {
                        Server.totalCommands.incrementAndGet();

                        
                        long uptimeSeconds = (System.currentTimeMillis() - Server.startTime) / 1000;

                        long totalCmds = Server.totalCommands.get();
                        long hits = Server.cacheHits.get();
                        long misses = Server.cacheMisses.get();
                        int dbSize = Server.map.size();
                        int clients = Server.connectedClients.get();

                        double hitRate = 0.0;
                        if ((hits + misses) > 0) {
                            hitRate = ((double) hits / (hits + misses)) * 100.0;
                        }

                        
                        StringBuilder info = new StringBuilder();
                        info.append(" === СТАТИСТИКА СЕРВЕРА ===\n");
                        info.append(String.format(" Время работы: %d сек.\n", uptimeSeconds));
                        info.append(String.format(" Активных подключений: %d\n", clients));
                        info.append("-----------------------------\n");
                        info.append(String.format(" Всего обработано команд: %d\n", totalCmds));
                        info.append(String.format(" Успешных чтений (hits): %d\n", hits));
                        info.append(String.format(" Промахов кэша (misses): %d\n", misses));
                        info.append(String.format(" Эффективность кэша: %.2f%%\n", hitRate));
                        info.append("-----------------------------\n");
                        info.append(String.format(" Всего ключей в памяти: %d\n", dbSize));
                        info.append("=============================\n");

                        out.println(info.toString());
                    }


                }
            }
        } catch (Exception e) {
            log.error("  ",e);
        }
    }
}
