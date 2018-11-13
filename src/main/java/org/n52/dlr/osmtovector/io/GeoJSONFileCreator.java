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

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class GeoJSONFileCreator {

    public static void writeGeoJSONFeatureCollection(File f, Geometry geom) throws IOException {

        FeatureTypeFactoryImpl factory = new FeatureTypeFactoryImpl();

        // create the featuretype
        SimpleFeatureType ft = null;
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder(factory);
        builder.setName("out");
        builder.setNamespaceURI("g");
        builder.setSRS("EPSG:4326");
        builder.add("geom", geom.getClass());
        builder.setDefaultGeometry("geom");

        ft = builder.buildFeatureType();

        SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(ft);
        fBuilder.add(geom);
        SimpleFeature feature = fBuilder.buildFeature("out");

        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        collection.add(feature);

        try (FileOutputStream outStream = new FileOutputStream(f)) {

            FeatureJSON fj = new FeatureJSON();
            fj.writeFeatureCollection(collection, outStream);
        }
    }
}
