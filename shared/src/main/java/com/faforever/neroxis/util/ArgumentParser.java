package com.faforever.neroxis.util;

import java.util.HashMap;
import java.util.Map;

public class ArgumentParser {
    /**
     * Parses command line arguments
     * Supports keys without value
     *
     * @param args The arguments as given by the system
     * @return A map mapping key -> value which before where formatted as '--key1 value1 --key2 value2 --key3'
     */
    public static Map<String, String> parse(String[] args) {
        Map<String, String> res = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("Wrong formatting of arguments. Expected: --");
            }

            String key = args[i].substring(2);
            res.put(key, null);

            if (i + 1 < args.length && (!args[i + 1].startsWith("--"))) {
                i++;
                res.put(key, args[i]);
            }
        }
        return res;
    }
}
