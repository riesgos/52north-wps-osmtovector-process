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

package org.n52.dlr.osmtovector.modules;

import org.n52.dlr.osmtovector.OSMToVectorProcessRepository;
import org.n52.dlr.osmtovector.algorithm.OSMDatasetList;
import org.n52.dlr.osmtovector.algorithm.OSMToVector;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.FormatEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OSMToVectorProcessRepositoryCM extends ClassKnowingModule {

    public static final String osmStoreDirectoryKey = "osm_store_directory";
    public static final String osmExtractBinaryKey = "osm_extract_binary";
    public static final String workDirectoryKey = "work_directory";
    private static Logger LOGGER = LoggerFactory.getLogger(OSMToVectorProcessRepositoryCM.class);
    private List<AlgorithmEntry> algorithmEntries;
    private boolean isActive = true;
    private ConfigurationEntry<String> osmInputFileEntry = new StringConfigurationEntry(
            osmStoreDirectoryKey,
            "OSM Input directory",
            "The directory containing the OSM input files. Only files ending with '.osm.pdf' will be used.",
            true,
            "/tmp"
    );

    private ConfigurationEntry<String> osmExtractBinaryEntry = new StringConfigurationEntry(
            osmExtractBinaryKey,
            "Path to the osm_extract binary",
            "",
            true,
            "osm_extract.py"
    );

    private ConfigurationEntry<String> workDirectoryEntry = new StringConfigurationEntry(
            workDirectoryKey,
            "work directory for temporary files",
            "",
            true,
            System.getProperty("java.io.tmpdir")
    );

    private List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(
            osmInputFileEntry,
            osmExtractBinaryEntry,
            workDirectoryEntry
    );

    public OSMToVectorProcessRepositoryCM() {
        algorithmEntries = new ArrayList<>();
        algorithmEntries.add(new AlgorithmEntry(OSMToVector.class.getName(), true)); // TODO: add only when missing
        algorithmEntries.add(new AlgorithmEntry(OSMDatasetList.class.getName(), true));
    }

    @Override
    public String getClassName() {
        return OSMToVectorProcessRepository.class.getName();
    }

    @Override
    public String getModuleName() {
        return "OSMToVectorProcessRepository Configuration Module";
    }

    @Override
    public boolean isActive() {
        return this.isActive;
    }

    @Override
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public ConfigurationCategory getCategory() {
        return ConfigurationCategory.REPOSITORY;
    }

    @Override
    public List<? extends ConfigurationEntry<?>> getConfigurationEntries() {
        return this.configurationEntries;
    }

    @Override
    public List<AlgorithmEntry> getAlgorithmEntries() {
        return this.algorithmEntries;
    }

    @Override
    public List<FormatEntry> getFormatEntries() {
        return null;
    }
}
