package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.MicroOutletDetails;

import java.util.UUID;

import org.jooq.impl.DSL;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBWriter;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_DETAILS;


public class MicroOutletDetailsService extends AbstractCDMService<MicroOutletDetails> {
    public static final String CACHE_DOMAIN = "microOutlets";


    public MicroOutletDetails save(MicroOutletDetails cdmObject) {
        if (cdmObject.getId() == null) {
            cdmObject.setId(UUID.randomUUID().toString());
        }

        fillCommonAttributes(cdmObject);

        var record = getDslContext().newRecord(CK_OUTLET_DETAILS, cdmObject);
        record.changed(CK_OUTLET_DETAILS.ID, false); // if you need to ignore changes
        record.store(); // INSERT with all POJO fields except geometry (leave null)

        GeometryFactory gf = new GeometryFactory();
        Point zeroPoint = gf.createPoint(new Coordinate(0.0, 0.0));
        zeroPoint.setSRID(4326);
        byte[] wkb = new WKBWriter().write(zeroPoint);

        getDslContext()
                .update(CK_OUTLET_DETAILS)
                .set(CK_OUTLET_DETAILS.COORDINATE,
                        DSL.field("ST_GeomFromWKB(?, ?)", CK_OUTLET_DETAILS.COORDINATE.getDataType(),
                                DSL.val(wkb), DSL.val(zeroPoint.getSRID())))
                .where(CK_OUTLET_DETAILS.ID.eq(cdmObject.getId()))
                .execute();

        return cdmObject;

    }

    public MicroOutletDetails findByOutletCode(String outletCode) {
        return getDslContext()
                .select(CK_OUTLET_DETAILS.asterisk().except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletCode))
                .fetchOneInto(MicroOutletDetails.class);
    }



}
