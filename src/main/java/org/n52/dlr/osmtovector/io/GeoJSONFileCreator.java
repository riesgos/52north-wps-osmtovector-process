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
