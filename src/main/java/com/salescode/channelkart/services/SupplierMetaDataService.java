package com.salescode.channelkart.services;



import com.salescode.channelkart.repository.impl.SupplierMetaDataRepository;
import com.salescode.jooq.CkSupplierMetadata;
import org.springframework.stereotype.Service;

@Service
public class SupplierMetaDataService extends AbstractCDMService<CkSupplierMetadata> {

	public SupplierMetaDataService(SupplierMetaDataRepository repository) {

	}
	
}
