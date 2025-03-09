package com.example.demo.util;

import java.util.Random;

public class ShortKeyGenerator {

    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();


    public static String generate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(BASE62.charAt((RANDOM.nextInt(BASE62.length()))));
        }
        return sb.toString();
    }

}
