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

package org.n52.dlr.osmtovector.algorithm;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.n52.dlr.osmtovector.OSMToVectorProcessRepository;
import org.n52.dlr.osmtovector.io.GeoJSONFileCreator;
import org.n52.dlr.osmtovector.io.IOUtil;
import org.n52.dlr.osmtovector.io.OSMDatasetStore;
import org.n52.dlr.osmtovector.modules.OSMToVectorProcessRepositoryCM;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Algorithm(
        version="1.0.0",
        title="OSMToVector",
        abstrakt = "Extract a subset of an OSM dataset"
)
public class OSMToVector extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(OSMToVector.class);
    private List<String> tags;
    private Geometry spatialFilter;
    private String exportLayerName = "export";
    private String exportFileName = "out";
    private String osmExtractBinary = "osm_extract.py";
    private String elementType;
    private FeatureCollection<?, ?> features;
    private String osmStoreDirectory;
    private String osmInputDataset;
    private File workDirectory;

    public OSMToVector() {
        super();
    }

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
            identifier = "elementType",
            abstrakt = "Type of OSM elements to export. Supported values are 'nodes' and 'ways'.",
            minOccurs = 0,
            maxOccurs = 1,
            defaultValue = "nodes",
            binding = LiteralStringBinding.class
    )
    public void setElementsToExport(String elementType){
        this.elementType = elementType;
    }

    @LiteralDataInput(
            identifier = "dataset",
            abstrakt = "Name of the OSM input dataset to use. Use the OSMDatasetList process to get a list",
            minOccurs = 1,
            maxOccurs = 1,
            binding = LiteralStringBinding.class
    )
    public void setInputDatasetName(String datasetName) {
        this.osmInputDataset = datasetName;
    }

    @ComplexDataOutput(
            binding = GTVectorDataBinding.class,
            identifier = "exportedData"
    )
    public FeatureCollection getExportedData() {
        return features;
    }

    private void setConfiguration() {
        ConfigurationModule cm = WPSConfig.getInstance().getConfigurationModuleForClass(
                OSMToVectorProcessRepository.class.getName(),
                ConfigurationCategory.REPOSITORY
        );

        for (ConfigurationEntry cEntry: cm.getConfigurationEntries()) {
           if (cEntry.getKey().equals(OSMToVectorProcessRepositoryCM.osmStoreDirectoryKey)) {
               this.osmStoreDirectory = (String) cEntry.getValue();
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
            args.add(exportLayerName);

            if (!tags.isEmpty()) {
                args.add("-t");
                for (String tag : tags) {
                    args.add(tag);
                }
            }

            args.add("-f");
            args.add("ESRI Shapefile");

            if (elementType.contentEquals("nodes")) {
                // the default - nothing to do
            } else if (elementType.contentEquals("ways")) {
                args.add("--ways");
                args.add("--length");
            } else {
                throw new ExceptionReport(
                        "Unsupported elementType: " + elementType
                                + ". Supported are 'ways', 'nodes'",
                        "invalid-args");
            }

            // set the spatial filter if there is one
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
            try {
                OSMDatasetStore store = new OSMDatasetStore(osmStoreDirectory);
                args.add(store.getPathForDataset(osmInputDataset).toAbsolutePath().toString());
            } catch (IOException e) {
                LOGGER.error("Could not find input dataset '" + osmInputDataset + "'", e);
                throw new ExceptionReport("Could not find input dataset '" + osmInputDataset + "'", "io", e);
            }

            // output file
            args.add(new File(tmpdir, exportFileName).getAbsolutePath());

            Runtime rt = Runtime.getRuntime();
            String[] argArr = new String[args.size()];
            argArr = args.toArray(argArr);
            String printableCommand = String.join(" ", argArr);
            LOGGER.info("Executing {}", printableCommand);

            Instant procStart = Instant.now();
            Process proc = rt.exec(argArr, new String[0], tmpdir);

            // wait for subprocess to finish
            try {
                int returnCode = proc.waitFor();

                if (returnCode != 0) {
                    // get the stderr of the command and write it to the logfile for problem diagnosis.
                    String errOutput = IOUtil.readInputStreamIntoString(proc.getErrorStream());
                    LOGGER.error("subprocess failed with returncode " + returnCode + ":\n" + errOutput);

                    throw new ExceptionReport("subprocess failed", "internal");
                }
            } catch (InterruptedException e) {
                LOGGER.error("subprocess was interrupted.", e);
                throw new ExceptionReport("Error handling processing request: subprocess was interrupted", "internal");
            } finally {
                proc.destroy();

                LOGGER.info("subprocess \"{}\" took {} seconds to execute",
                        printableCommand,
                        Duration.between(procStart, Instant.now()).toMillis() / 1000.0);
            }

            // get result from disk
            DataStore store = new ShapefileDataStore(new File(tmpdir, exportFileName + "/" + exportLayerName + ".shp").toURI().toURL());
            features = store.getFeatureSource(store.getTypeNames()[0]).getFeatures();

        } catch (IOException e) {
            LOGGER.error("could not process", e);
            throw new ExceptionReport("Error handling processing request: " + e.getMessage(), "internal");
        } finally {
            if (tmpdir != null) {
                if (tmpdir.exists()) {
                    IOUtil.recursiveDelete(tmpdir);
                }
            }
        }
    }
}
