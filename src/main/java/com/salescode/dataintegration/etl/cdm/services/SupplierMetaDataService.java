package com.salescode.dataintegration.etl.cdm.services;

import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.repository.SupplierMetaDataRepository;
import com.salescode.jooq.CkSupplierMetadata;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Service;

import static com.salescode.jooq.generated.Tables.CK_SUPPLIER_METADATA;

@Service
public class SupplierMetaDataService extends AbstractCDMService<CkSupplierMetadata> {

    public SupplierMetaDataService(DSLContext dslcontext, SupplierMetaDataRepository repository) {
        super(dslcontext);

    }

    @Override
    protected Table<? extends Record> getTable() {
        return CK_SUPPLIER_METADATA;
    }
}
