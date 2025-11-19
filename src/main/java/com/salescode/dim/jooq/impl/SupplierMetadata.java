package com.salescode.dim.jooq.impl;



import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierMetadata extends com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata implements Serializable {
  private User user;
}
