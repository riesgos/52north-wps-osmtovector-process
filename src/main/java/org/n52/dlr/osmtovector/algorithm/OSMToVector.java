package org.n52.dlr.osmtovector.algorithm;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.n52.dlr.osmtovector.OSMToVectorProcessRepository;
import org.n52.dlr.osmtovector.modules.OSMToVectorProcessRepositoryCM;
import org.n52.dlr.osmtovector.util.FileUtil;
import org.n52.dlr.osmtovector.util.GeoJSONFileCreator;
import org.n52.wps.algorithm.annotation.*;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.JTSGeometryBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


@Algorithm(
        version="1.0.0",
        title="OSMToVector"
)
public class OSMToVector extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(OSMToVector.class);
    private List<String> tags;
    private Geometry spatialFilter;
    private String layerName = "export";
    private String osmExtractBinary = "osm_extract.py";
    private String exportType;
    private FeatureCollection<?, ?> features;
    private String osmInputFileName;
    private File workDirectory;

    @LiteralDataInput(
            identifier = "tag",
            abstrakt = "OSM tags to extract. Each tag will become its own attribute in the result",
            minOccurs = 0,
            maxOccurs = 50,
            binding = LiteralStringBinding.class
    )
    public void setTag(List<String> tags) {
        this.tags = tags;
    }

    @ComplexDataInput(
            identifier = "spatialFilter",
            abstrakt = "Polygon or MultiPolygon geometry of the area of interest",
            minOccurs = 0,
            maxOccurs = 1,
            binding = JTSGeometryBinding.class
    )
    public void setSpatialFilter(Geometry geom) {
        this.spatialFilter = geom;
    }

    @LiteralDataInput(
            identifier = "exportType",
            abstrakt = "Type of OSM data to export. Possible values are 'nodes' and 'ways'.",
            minOccurs = 0,
            maxOccurs = 1,
            defaultValue = "nodes",
            binding = LiteralStringBinding.class
    )
    public void setTypeToExport(String exportType){
        this.exportType = exportType;
    }

    @ComplexDataOutput(
            binding = GTVectorDataBinding.class,
            identifier = "exportedData"
    )
    public FeatureCollection getExportedData() {
        return features;
    }

    public OSMToVector() {
        super();
    }

    private void setConfiguration() {
        ConfigurationModule cm = WPSConfig.getInstance().getConfigurationModuleForClass(
                OSMToVectorProcessRepository.class.getName(),
                ConfigurationCategory.REPOSITORY
        );

        for (ConfigurationEntry cEntry: cm.getConfigurationEntries()) {
           if (cEntry.getKey().equals(OSMToVectorProcessRepositoryCM.osmInputFileKey)) {
               this.osmInputFileName = (String) cEntry.getValue();
           } else if (cEntry.getKey().equals(OSMToVectorProcessRepositoryCM.osmExtractBinaryKey)) {
               this.osmExtractBinary = (String) cEntry.getValue();
           } else if (cEntry.getKey().equals(OSMToVectorProcessRepositoryCM.workDirectoryKey)) {
               this.workDirectory = new File((String) cEntry.getValue());
           }
        }
    }

    @Execute
    public void run() throws ExceptionReport {
        this.setConfiguration();

        File tmpdir = null;
        try {
            tmpdir = Files.createTempDirectory(this.workDirectory.toPath(), "osmtovector").toFile();

            List<String> args = new ArrayList<>();
            args.add(this.osmExtractBinary);
            args.add("-l");
            args.add(layerName);

            if (!tags.isEmpty()) {
                args.add("-t");
                for (String tag : tags) {
                    args.add(tag);
                }
            }

            args.add("-f");
            args.add("ESRI Shapefile");

            if (exportType.contentEquals("nodes")) {
                // the default - nothing to do
            } else if (exportType.contentEquals("ways")) {
                args.add("--ways");
                args.add("--length");
            } else {
                throw new ExceptionReport(
                        "Unsupported export type: " + spatialFilter.getGeometryType()
                                + ". Supported are 'ways', 'nodes'",
                        "invalid-args");
            }

            if (spatialFilter != null) {
                if (!spatialFilter.getGeometryType().equals("Polygon") && !spatialFilter.getGeometryType().equals("MultiPolygon")) {
                    throw new ExceptionReport(
                            "Unsupported geometry type for the spatialFilter: " + spatialFilter.getGeometryType(),
                            "invalid-args");
                }

                File spatialFilterFile = new File(tmpdir, "filter.geojson");
                GeoJSONFileCreator.writeGeoJSONFeatureCollection(spatialFilterFile, spatialFilter);

                args.add("--geofilter");
                args.add(spatialFilterFile.getAbsolutePath());
            }

            // input file
            args.add(this.osmInputFileName);

            // output file
            args.add(new File(tmpdir, "out").getAbsolutePath());

            Runtime rt = Runtime.getRuntime();
            String[] argArr = new String[args.size()];
            argArr = args.toArray(argArr);
            LOGGER.info("Executing " + String.join(" ", argArr));
            Process proc = rt.exec(argArr, new String[0], tmpdir);

            // wait for algorithm to finish
            try {
                int returnCode = proc.waitFor();

                if (returnCode != 0) {
                    // get the stderr of the command and write it
                    // to the logfile for problem diagnosis.
                    Scanner scanner = new Scanner(proc.getErrorStream()).useDelimiter("\\A");
                    String errOutput = scanner.hasNext() ? scanner.next() : "";
                    LOGGER.error("subprocess failed with returncode " + returnCode + ": " + errOutput);

                    throw new ExceptionReport("subprocess failed", "internal");
                }
            } catch (InterruptedException e) {
                LOGGER.error("subprocess was interrupted.", e);
                throw new ExceptionReport("Error handling processing request: subprocess was interrupted", "internal");
            } finally {
                proc.destroy();
            }

            // get result
            DataStore store = new ShapefileDataStore(new File(tmpdir, "out/" + layerName + ".shp").toURI().toURL());
            features = store.getFeatureSource(store.getTypeNames()[0]).getFeatures();

        } catch (IOException e) {
            LOGGER.error("could not process", e);
            throw new ExceptionReport("Error handling processing request: " + e.getMessage(), "internal");
        } finally {
            if (tmpdir != null) {
                if (tmpdir.exists()) {
                    FileUtil.recursiveDelete(tmpdir);
                }
            }
        }
    }
}
