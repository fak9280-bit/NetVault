package com;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLServerSocket;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Server {
    public static final java.util.concurrent.atomic.AtomicInteger connectedClients = new java.util.concurrent.atomic.AtomicInteger(0);
    static AtomicLong totalCommands = new AtomicLong(0);
    static AtomicLong cacheHits = new AtomicLong(0);
    static AtomicLong cacheMisses = new AtomicLong(0);
    static long startTime = System.currentTimeMillis();
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private static int PORT= Integer.parseInt(ConfigManager.getPort());
    static Map<String, DataRecord> map = new ConcurrentHashMap<>();
    private static final ExecutorService threadPool =Executors.newFixedThreadPool(20);
    public static void socketServer() throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {


        log.info("Загрузка базы");

        map=Converttojson.loadFromFile();

        log.info("База загружена");

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream ksStream = Server.class.getResourceAsStream("/keystore.p12")) {
                if (ksStream == null) {
                    throw new IllegalStateException("Файл keystore.p12 не найден в resources!");
                }
                keyStore.load(ksStream, "changeit".toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "changeit".toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

            InetSocketAddress address = new InetSocketAddress(ConfigManager.getHost(), PORT);
            try (SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket())
            {
               serverSocket.bind(address);

                log.info("Сервер запущен с TLS и слушает порт {}", PORT);
                while (true) {
                    log.info("Ожидание подключения...");

                    Socket clientSocket = serverSocket.accept();

                    log.info("Подключен :" + clientSocket.getInetAddress());
                    connectedClients.incrementAndGet();
                    threadPool.execute(() -> {
                        try {
                            HandClient.handleClient(clientSocket);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            }
        }
        } catch (Exception e) {
            log.error("Критическая ошибка при запуске TLS-сервера", e);
        }

    }

 }


