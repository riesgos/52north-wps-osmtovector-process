/*
 * Copyright 2018 Deutsches Zentrum für Luft- und Raumfahrt e.V.
 *         (German Aerospace Center), German Remote Sensing Data Center
 *         Department: Geo-Risks and Civil Security
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
