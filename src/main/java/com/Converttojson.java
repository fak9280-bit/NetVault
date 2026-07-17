package com;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Converttojson  {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static File file =new File("Data.json");
     public static void savetosjon(Map<String,String> data) throws IOException {

         objectMapper.writeValue(file,data);

     }
     public static Map<String,String> loadtojson() throws IOException {
         if (file.exists() && file.length()>0){

             return objectMapper.readValue(file, new TypeReference<Map<String,String>>() {});
         }
         else {
             return new ConcurrentHashMap<>();
         }
     }
}
