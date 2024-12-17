/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.salescode.channelkart.repository;


import com.salescode.channelkart.models.SequenceInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;


/**
 * The interface SequenceInfoRepository.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
@Repository
public interface SequenceInfoRepository extends CommonJpaRepository<SequenceInfo, String> {

	/**
	 * Find by entity and field name.
	 *
	 * @param entity the entity
	 * @param fieldName the field name
	 * @param type the type
	 * @return the sequence info
	 */
	public SequenceInfo findByEntityAndFieldNameAndType(String entity, String fieldName, String type);

	@Transactional
	@Query(value = "{CALL getnextval(:seqname,@sequencenumber)}", nativeQuery = true)
	String executeSequenceProcedures(@Param("seqname") String seqname);

	@Transactional
	@Modifying
	@Query(value = "{CALL createsequence(:seqname,:startvalue)}", nativeQuery = true)
	int createSequenceProcedure(@Param("seqname") String seqname, @Param("startvalue") Long startvalue);

}
