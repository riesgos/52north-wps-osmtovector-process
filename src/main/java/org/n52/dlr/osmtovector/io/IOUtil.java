package org.n52.dlr.osmtovector.io;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.Scanner;

public class IOUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);


    public static void recursiveDelete(File f) {
        if (f.isDirectory()) {
            for (File content: f.listFiles()) {
                recursiveDelete(content);
            }
        }
        if (!f.delete()) {
            LOGGER.warn("Could not delete temporary file: "+ f.getAbsolutePath());
        }
    }

    public static String readInputStreamIntoString(InputStream is) {
        try (Scanner scanner = new Scanner(is)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
