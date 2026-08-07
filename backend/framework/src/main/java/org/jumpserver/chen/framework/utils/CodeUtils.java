package org.jumpserver.chen.framework.utils;


public class CodeUtils {

    /**
     * Escape newline characters for the exported file
     * @param value
     * @return
     */
    public static String escapeCsvValue(String value) {
        if (value.contains("\"") || value.contains(",") || value.contains("\n")) {
            // Escape quotes
            value = value.replace("\"", "\"\"");
            // Wrap the value in quotes
            value = "\"" + value + "\"";
        }
        return value;
    }
}
