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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OSMDatasetStore {

    public static String fileNameExtension = ".osm.pbf";
    private Path directory;
    private Map<String, Path> datasetMap = new HashMap<>();

    public OSMDatasetStore(String directory) throws IOException {
        this.directory = Paths.get(directory);
        scanDirectory();
    }

    public void scanDirectory() throws IOException {
        datasetMap.clear();

        DirectoryStream<Path> stream = Files.newDirectoryStream(this.directory, "*"+ fileNameExtension);
        for (Path path: stream) {
            datasetMap.put(getDatasetName(path), path);
        }
    }

    private String getDatasetName(Path path) {
        return path.getName(path.getNameCount()-1).toString()
                .replace(fileNameExtension, "")
                .replaceAll("[^a-zA-Z0-9\\.\\-_]", "");
    }

    public List<String> getDatasetList() {
        ArrayList<String> results = new ArrayList<String>();
        results.addAll(datasetMap.keySet());
        return results;
    }

    public Path getPathForDataset(String datasetName) throws IOException {
        Path path = datasetMap.get(datasetName);
        if (path == null || (!path.toFile().exists())) {
            throw new IOException("OSM dataset with the name " + datasetName + " does not exist");
        }
        return path;
    }
}
