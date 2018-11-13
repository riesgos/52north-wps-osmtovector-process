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

    private Path directory;
    private Map<String, Path> datasetMap = new HashMap<>();

    public static String fileNameExtension = ".osm.pbf";

    public OSMDatasetStore(String directory) throws IOException {
        this.directory = Paths.get(directory);
        scanDirectory();
    }

    public void scanDirectory() throws IOException {
        datasetMap.clear();

        List<String> providedDatasets = new ArrayList<>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(this.directory, "*"+ fileNameExtension);
        for (Path path: stream) {
            datasetMap.put(getDatasetName(path), path);
        }
    }

    private String getDatasetName(Path path) {
        return path.getName(path.getNameCount()-1).toString()
                .replaceAll(fileNameExtension, "")
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
