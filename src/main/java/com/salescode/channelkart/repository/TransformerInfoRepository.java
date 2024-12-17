package com.salescode.channelkart.repository;

import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.transformers.TransformerInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Stream;

@Repository
public interface TransformerInfoRepository extends CommonJpaRepository<TransformerInfo, String>
{
	// Return Transformer list
	public List<TransformerInfo> findAll();

	TransformerInfo findByIdAndActiveStatus(String id, ActiveStatus activeStatus);

	@Query("select id from TransformerInfo where name = ?1 and activeStatus = 'active'")
	String findIdByName(@NotNull @NotBlank String name);

	Stream<TransformerInfo> streamAllByActiveStatus(ActiveStatus activeStatus);

	List<TransformerInfo> findAllByActiveStatus(ActiveStatus activeStatus);
}
