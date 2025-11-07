package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.ApprovalInfoRepository;
import com.salescode.dim.jooq.generated.tables.pojos.ApprovalInfo;

import java.util.List;

public class ApprovalInfoService extends AbstractCDMService<ApprovalInfo> {

	public ApprovalInfoService() {
	}

	private ApprovalInfoRepository approvalInfoRepository;

	public ApprovalInfoService(ApprovalInfoRepository approvalInfoRepository) {
		this.approvalInfoRepository = approvalInfoRepository;
	}

	public List<ApprovalInfo> findByReferenceIdList(List<String> referenceIdList) {
		return approvalInfoRepository.findByReferenceIdIn(referenceIdList);
	}
}
