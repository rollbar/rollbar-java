package com.rollbar.payload.utilities;

import java.io.IOException;
import java.io.InputStream;

/**
 * A class that gets properties from the properties file.
 */
public class Properties {
    /**
     * Get the assembly verison in the assembly.properties file. Should update automatically with each Maven release.
     * @return the version
     */
    public static String getAssemblyVersion() {
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
}
