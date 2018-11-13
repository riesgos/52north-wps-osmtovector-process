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

import org.n52.dlr.osmtovector.OSMToVectorProcessRepository;
import org.n52.dlr.osmtovector.modules.OSMToVectorProcessRepositoryCM;
import org.n52.dlr.osmtovector.io.OSMDatasetStore;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Algorithm(
        version="1.0.0",
        title="OSMDatasetList",
        abstrakt = "List the OSM datasets the server provides"
)
public class OSMDatasetList extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(OSMDatasetList.class);

    private List<String> providedDatasets = new ArrayList<>();
    private String osmStoreDirectory;

    public OSMDatasetList() { super(); }

    @LiteralDataOutput(
            identifier = "datasets",
            binding = LiteralStringBinding.class
    )
    public String getDatasets() {
        return String.join(",", providedDatasets);
    }

    private void setConfiguration() {
        ConfigurationModule cm = WPSConfig.getInstance().getConfigurationModuleForClass(
                OSMToVectorProcessRepository.class.getName(),
                ConfigurationCategory.REPOSITORY
        );

        for (ConfigurationEntry cEntry: cm.getConfigurationEntries()) {
            if (cEntry.getKey().equals(OSMToVectorProcessRepositoryCM.osmStoreDirectoryKey)) {
                this.osmStoreDirectory = (String) cEntry.getValue();
            }
        }
    }

    @Execute
    public void run() throws ExceptionReport {
        this.setConfiguration();

        try {
            OSMDatasetStore store = new OSMDatasetStore(osmStoreDirectory);
            providedDatasets = store.getDatasetList();
        } catch (IOException e) {
            LOGGER.error("Could not list directory of input files '"+ osmStoreDirectory +"'", e);
            throw new ExceptionReport("Could not list input file directory", "io", e);
        }
    }
}
