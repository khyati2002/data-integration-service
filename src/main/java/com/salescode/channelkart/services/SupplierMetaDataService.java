package com.salescode.channelkart.services;




import com.salescode.channelkart.models.SupplierMetaData;
import com.salescode.channelkart.repository.SupplierMetaDataRepository;
import org.springframework.stereotype.Service;

@Service
public class SupplierMetaDataService extends AbstractCDMService<SupplierMetaData>{

	public SupplierMetaDataService(SupplierMetaDataRepository repository) {
		super(repository);
	}

}
