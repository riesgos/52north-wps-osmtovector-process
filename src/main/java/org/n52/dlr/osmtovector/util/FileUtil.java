package org.n52.dlr.osmtovector.util;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;

public class FileUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);


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
}
