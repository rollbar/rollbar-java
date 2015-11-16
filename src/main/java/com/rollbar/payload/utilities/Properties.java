package com.rollbar.payload.utilities;

import java.io.IOException;
import java.io.InputStream;

public class Properties {
    private static String getAssemblyVersion() {
        final String file = "assembly.properties";
        try {
            InputStream stream = Properties.class
                    .getClassLoader()
                    .getResourceAsStream(file);
            java.util.Properties properties = new java.util.Properties();
            if (stream != null) {
                properties.load(stream);
            }
            else {
                System.err.println("File not found: " + file);
                return null;
            }
            return properties.getProperty("version");
        }
        catch (IOException e){
            System.err.println("File read error: " + file);
            return null;
        }
    }

    public static final String assemblyVersion = getAssemblyVersion();
}
