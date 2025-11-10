package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.ApprovalInfoRepository;
import com.salescode.dim.jooq.impl.ApprovalInfo;

import java.util.List;

public class ApprovalInfoService extends AbstractCDMService<ApprovalInfo> {

	private final ApprovalInfoRepository approvalInfoRepository;

	public ApprovalInfoService(ApprovalInfoRepository approvalInfoRepository) {
		this.approvalInfoRepository = approvalInfoRepository;
	}

	@Override
	public Class<ApprovalInfo> getPersistentClass() {
		return ApprovalInfo.class;
	}

	public List<ApprovalInfo> findByReferenceIdList(List<String> referenceIdList) {
		return approvalInfoRepository.findByReferenceIdIn(referenceIdList);
	}
}
