package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.UserMetadata;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserMetadata extends com.salescode.dim.jooq.generated.tables.pojos.UserMetadata implements Serializable {

    public UserMetadata() {
        super();
    }

    public UserMetadata(com.salescode.dim.jooq.generated.tables.pojos.UserMetadata userMetadata) {
        super(userMetadata);
    }

    public static UserMetadata of(com.salescode.dim.jooq.generated.tables.pojos.UserMetadata userMetadata) {
        if (userMetadata == null) {
            return null;
        }
        return new UserMetadata(userMetadata);
    }
}