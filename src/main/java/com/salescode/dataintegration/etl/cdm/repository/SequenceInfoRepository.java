/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.salescode.dataintegration.etl.cdm.repository;

import com.salescode.jooq.generated.tables.pojos.CkSequenceInfo;

import org.springframework.stereotype.Repository;



/**
 * The interface SequenceInfoRepository.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
@Repository
public interface SequenceInfoRepository  {

	/**
	 * Find by entity and field name.
	 *
	 * @param entity the entity
	 * @param fieldName the field name
	 * @param type the type
	 * @return the sequence info
	 */
	public CkSequenceInfo findByEntityAndFieldNameAndType(String entity, String fieldName, String type);


	String executeSequenceProcedures(String seqname);


	int createSequenceProcedure(String seqname,Long startvalue);

}
