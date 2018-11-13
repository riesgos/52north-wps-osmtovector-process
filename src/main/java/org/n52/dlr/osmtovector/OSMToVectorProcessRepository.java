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

package org.n52.dlr.osmtovector;

import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.IAlgorithm;
import org.n52.wps.server.IAlgorithmRepository;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class OSMToVectorProcessRepository implements IAlgorithmRepository {

    private static Logger LOGGER = LoggerFactory.getLogger(OSMToVectorProcessRepository.class);
    private Map<String, IAlgorithm> algorithmMap;
    private Map<String, ProcessDescription> processDescriptionMap;
    private ConfigurationModule cm;

    public OSMToVectorProcessRepository() {
        LOGGER.info("Initializing OSMToVector Repository");

        algorithmMap = new HashMap<>();
        processDescriptionMap = new HashMap<>();

        cm = WPSConfig.getInstance().getConfigurationModuleForClass(
                this.getClass().getName(),
                ConfigurationCategory.REPOSITORY
        );

        if (cm.isActive()) {
            for (AlgorithmEntry algorithmEntry : cm.getAlgorithmEntries()) {
                if (algorithmEntry.isActive()) {
                    addAlgorithm(algorithmEntry.getAlgorithm());
                }
            }
        } else {
            LOGGER.info("repository is inactive");
        }
    }

    private IAlgorithm loadAlgorithm(String algorithmClassName) throws Exception {
        Class<?> algorithmClass = OSMToVectorProcessRepository.class.getClassLoader().loadClass(algorithmClassName);
        IAlgorithm algorithm = null;
        if (IAlgorithm.class.isAssignableFrom(algorithmClass)) {
            algorithm = IAlgorithm.class.cast(algorithmClass.newInstance());
        } else if (algorithmClass.isAnnotationPresent(Algorithm.class)) {
            // we have an annotated algorithm that doesn't implement IAlgorithm
            // wrap it in a proxy class
            algorithm = new AbstractAnnotatedAlgorithm.Proxy(algorithmClass);
        } else {
            throw new Exception(
                    "Could not load algorithm "
                            + algorithmClassName
                            + " does not implement IAlgorithm or have a Algorithm annotation.");
        }

        boolean isNoProcessDescriptionValid = false;

        for (String supportedVersion : WPSConfig.SUPPORTED_VERSIONS) {
            isNoProcessDescriptionValid = isNoProcessDescriptionValid
                    && !algorithm.processDescriptionIsValid(supportedVersion);
        }

        if (isNoProcessDescriptionValid) {
            LOGGER.warn("Algorithm description is not valid: " + algorithmClassName);
            throw new Exception("Could not load algorithm " + algorithmClassName + ". ProcessDescription Not Valid.");
        }
        return algorithm;
    }

    public boolean addAlgorithm(Object processID) {
        if (!(processID instanceof String)) {
            return false;
        }
        String algorithmClassName = (String) processID;

        try {
            IAlgorithm algorithm = loadAlgorithm(algorithmClassName);

            processDescriptionMap.put(algorithmClassName, algorithm.getDescription());
            algorithmMap.put(algorithmClassName, algorithm);
            LOGGER.info("Algorithm class registered: " + algorithmClassName);

            return true;
        } catch (Exception e) {
            LOGGER.error("Exception while trying to add algorithm " + algorithmClassName, e);
        }
        return false;
    }

    @Override
    public Collection<String> getAlgorithmNames() {
        Collection<String> names = new ArrayList<>();

        for (AlgorithmEntry algorithmEntry : cm.getAlgorithmEntries()) {
            if (algorithmEntry.isActive()) {
                names.add(algorithmEntry.getAlgorithm());
            }
        }
        return names;
    }

    @Override
    public IAlgorithm getAlgorithm(String className) {
        if (getAlgorithmNames().contains(className)) {
            return algorithmMap.get(className);
        }
        return null;
    }

    @Override
    public ProcessDescription getProcessDescription(String className) {
        if (getAlgorithmNames().contains(className)) {
            return processDescriptionMap.get(className);
        }
        return null;
    }

    @Override
    public boolean containsAlgorithm(String className) {
        return getAlgorithmNames().contains(className);
    }

    @Override
    public void shutdown() {
        // not implemented
    }
}
