package com.example.study;

import java.util.HashMap;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public final static String SERVICE_STRING = "F000C0E0-0451-4000-B000-000000000000";
    public final static String CHARACTERISTIC_STRING = "F000C0E1-0451-4000-B000-000000000000";

    static {
        // Services.
        attributes.put("F000C0E0-0451-4000-B000-000000000000", "Service");
        // Characteristics.
        attributes.put("F000C0E1-0451-4000-B000-000000000000", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
