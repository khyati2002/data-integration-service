package com.applicate.services.channelkart.enrichments.repository;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.UserMetadata;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class UserMetadataLocationEnrichment extends AbstractEnrichment<UserMetadata> {
	private final GeometryFactory geometryFactory = new GeometryFactory();

	public OperationResult.StepResult apply(UserMetadata userMetadata) {
		if (userMetadata.getLocation() == null) {
			double latitude = this.getLatitude(userMetadata);
			double longitude = this.getLongitude(userMetadata);
			Point point = this.geometryFactory.createPoint(new Coordinate(longitude, latitude));
			userMetadata.setLocation(point);
		}

		return new OperationResult.StepResult(OperationResult.Status.OK, "Score Status enriched");
	}

	private double getLatitude(UserMetadata userMetadata) {
		return userMetadata.getLatitude() == null ? 0.0 : userMetadata.getLatitude().doubleValue();
	}

	private double getLongitude(UserMetadata userMetadata) {
		return userMetadata.getLongitude() == null ? 0.0 : userMetadata.getLongitude().doubleValue();
	}

}
