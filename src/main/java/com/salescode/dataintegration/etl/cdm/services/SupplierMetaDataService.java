package com.salescode.dataintegration.etl.cdm.services;



import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.repository.SupplierMetaDataRepository;
import com.salescode.jooq.CkSupplierMetadata;
import org.springframework.stereotype.Service;

@Service
public class SupplierMetaDataService extends AbstractCDMService<CkSupplierMetadata> {

	public SupplierMetaDataService(SupplierMetaDataRepository repository) {

	}
	
}
