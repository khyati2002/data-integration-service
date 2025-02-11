package com.salescode.channelkart.services;



import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.SupplierMetaDataRepository;
import com.salescode.jooq.CkSupplierMetadata;
import org.springframework.stereotype.Service;

@Service
public class SupplierMetaDataService extends AbstractCDMService<CkSupplierMetadata> {

	public SupplierMetaDataService(SupplierMetaDataRepository repository) {

	}
	
}
