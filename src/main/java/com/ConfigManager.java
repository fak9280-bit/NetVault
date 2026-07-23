package com;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static final Properties properties = new Properties();

    static {

        try {
            InputStream inputStream = ConfigManager.class.getResourceAsStream("/config.properties");

            if(inputStream==null)
            {
                throw new IllegalStateException("Файл config.properties не найден в resources!");

            }
            properties.load(inputStream);

            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDbName(){
        return properties.getProperty("db.file.name");
    }
    public static String getCryptokey(){
        return properties.getProperty("crypto.secret.key");
    }
    public static String getPort(){
        return properties.getProperty("port");
    }
    public static String getHost(){
        return properties.getProperty("host");
    }
}
