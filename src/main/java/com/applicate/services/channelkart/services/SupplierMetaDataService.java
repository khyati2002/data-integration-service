package com.applicate.services.channelkart.services;




import com.applicate.services.channelkart.models.SupplierMetaData;
import com.applicate.services.channelkart.repository.SupplierMetaDataRepository;
import org.springframework.stereotype.Service;

@Service
public class SupplierMetaDataService extends AbstractCDMService<SupplierMetaData>{

	public SupplierMetaDataService(SupplierMetaDataRepository repository) {
		super(repository);
	}

}
