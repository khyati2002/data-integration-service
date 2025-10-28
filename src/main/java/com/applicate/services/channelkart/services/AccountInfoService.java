package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.impl.AccountInfo;
import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import lombok.Data;


import java.util.*;

@Data
public class AccountInfoService extends AbstractCDMService<AccountInfo> {



	private PropertyRegistry propertyRegistry;


	AesGcmKmsEncryptionService aesGcmKmsEncryptionService;

	private AccountInfoRepository accountInfoRepository;

	private static final AESEncryptionService aesEncryptionService = new AESEncryptionService();



	public AccountInfo findByLoginId(String loginId){
		return accountInfoRepository.findByLoginId(loginId);
	}


	public  AccountInfo decryptAccount(AccountInfo info){
		AccountInfo ainfo;
		String accountPayload = info.getAccountPayload();
		ainfo = JSONUtils.parse(decryptAccount(accountPayload),AccountInfo.class);
		ainfo.setVersion(info.getVersion());
		ainfo.setLastModifiedTime(info.getLastModifiedTime());
		ainfo.setLoginId(info.getLoginId());
		ainfo.setAccountNumber(info.getAccountNumber());
		ainfo.setAccountId(info.getAccountId());
		return ainfo;
	}

	public String decryptAccount(String accountPayload) {
		String ainfo;
		if(propertyRegistry.getAsBoolean(PropertyDefinition.ENABLE_AES_GCM_KMS_ENCRYPTION)){
			ainfo = aesGcmKmsEncryptionService.decrypt(accountPayload);
		}
		else{
			String key = System.getProperty("accountEncryptionKey", "letsencryptappck");
			ainfo = aesEncryptionService
					.decrypt(AESEncryptionService.decryptBase64(accountPayload), key.getBytes());
		}
		return ainfo;
	}


}
