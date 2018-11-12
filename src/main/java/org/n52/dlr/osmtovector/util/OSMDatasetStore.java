package org.n52.dlr.osmtovector.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OSMDatasetStore {

    private String directory;

    public static String fileNameExtension = ".osm.pbf";

    public OSMDatasetStore(String directory) {
        this.directory = directory;
    }

    public String getDatasetName(Path path) {
        return path.getName(path.getNameCount()-1).toString().replaceAll(fileNameExtension, "");
    }

    public List<String> getDatasetList() throws IOException  {

        Path inputDir = Paths.get(this.directory);

        List<String> providedDatasets = new ArrayList<>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*"+ fileNameExtension);
        for (Path path: stream) {
            providedDatasets.add(getDatasetName(path));
        }
        return providedDatasets;
    }

    public Path getPathForDataset(String datasetName) throws IOException {
        Path path = Paths.get(this.directory, datasetName.replaceAll("/", "") + fileNameExtension);
        if (!path.toFile().isFile()) {
            throw new IOException("OSM dataset with the name " + datasetName + " does not exist");
        }
        return path;
    }
}
