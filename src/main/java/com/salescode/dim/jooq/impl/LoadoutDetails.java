package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.DmsLoadoutDetails;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LoadoutDetails extends DmsLoadoutDetails {

    private List<LoadoutItems> loadoutItems;
}
