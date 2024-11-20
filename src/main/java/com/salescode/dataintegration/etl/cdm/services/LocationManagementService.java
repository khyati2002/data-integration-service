//package com.salescode.dataintegration.etl.cdm.services;
//
//import com.applicate.services.channelkart.models.Location;
//import com.applicate.services.channelkart.validations.RuleResult;
//import com.applicate.services.channelkart.validations.Status;
//import com.applicate.services.channelkart.validations.repository.FormValidator;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import javax.validation.ConstraintViolation;
//import java.util.List;
//import java.util.Set;
//
//@Service
//public class LocationManagementService {
//
//	@Autowired
//	FormValidator locationValidator;
//
//	public RuleResult  validateProductDetails(List<Location> locations) {
//		StringBuffer buffer=new StringBuffer();
//		for (int i = 0; i < locations.size(); i++) {
//			Set<ConstraintViolation<Location>> constraintViolations=locationValidator.formValidation(locations.get(i));
//			for (ConstraintViolation<Location> violation : constraintViolations) {
//				buffer.append(violation.getPropertyPath()+" "+violation.getMessage()+",");
//				}
//			if(buffer.length()!=0) {
//				break;
//			}
//			}
//			if(buffer.length()!=0)
//			{
//				return new RuleResult(Status.ERROR,buffer.toString());
//			}
//			else
//			{
//				return  RuleResult.OK;
//			}
//	}
//
//}
