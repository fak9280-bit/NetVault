package com;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;


public class Converttojson  {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static SecretKeySpec secretKeySpec =new SecretKeySpec(ConfigManager.getCryptokey().getBytes(StandardCharsets.UTF_8),"AES");

    static File file =new File(ConfigManager.getDbName());
     public static void saveToJson(Map<String, DataRecord> data) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

      String json=objectMapper.writeValueAsString(data);
      String encrypted = encrypto(json);
      try(FileWriter writer =new FileWriter(file)) {
                writer.write(encrypted);
      }

     }
     public static Map<String, DataRecord> loadFromFile() throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
         if (file.exists() && file.length()>0){
             String encrypted =new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
             String decrypted = decrypto(encrypted);

             return objectMapper.readValue(decrypted, new TypeReference<Map<String,DataRecord>>() {});
         }
         else {
             return new ConcurrentHashMap<>();
         }
     }

     public static String encrypto(String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {


         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(Cipher.ENCRYPT_MODE,secretKeySpec);
         byte[] ebcryptoBytes =cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
         return Base64.getEncoder().encodeToString(ebcryptoBytes);

     }
     public static String decrypto(String encryptoData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(Cipher.DECRYPT_MODE,secretKeySpec);
         byte[] decodedBytes =Base64.getDecoder().decode(encryptoData);
         byte[] decrypteBytes =cipher.doFinal(decodedBytes);
         return new String(decrypteBytes,StandardCharsets.UTF_8);
     }
}
