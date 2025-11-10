package com.applicate.services.channelkart.enrichments.repository;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.UserMetadata;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;

public class UserMetadataLocationEnrichment extends AbstractEnrichment<UserMetadata> {
    
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * Creates a JTS Point from latitude and longitude if the location object is null.
     * Defaults to 0.0 if latitude or longitude are null.
     */
    @Override
    public OperationResult.StepResult apply(UserMetadata userMetadata) {
        if (userMetadata.getLocation() == null) {
            BigDecimal latitude = userMetadata.getLatitude() == null ? BigDecimal.ZERO : userMetadata.getLatitude();
            BigDecimal longitude = userMetadata.getLongitude() == null ? BigDecimal.ZERO : userMetadata.getLongitude();

            Point point = geometryFactory.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
            userMetadata.setLocation(point);
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, "Location object enriched");
    }
}