package com.salescode.dataintegration.etl.cdm.repository;

import com.salescode.jooq.generated.tables.pojos.CkAuthRole;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository {

	List<CkAuthRole> findByName(String name);

	CkAuthRole findByNameIgnoreCaseContaining(String name);

	CkAuthRole findByNameIgnoreCase(String name);

}