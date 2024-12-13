package com.salescode.channelkart.repository;


import com.salescode.channelkart.models.Role;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends CommonJpaRepository<Role, String>{

	List<Role> findByName(String name);

	Role findByNameIgnoreCaseContaining(String name);

	Role findByNameIgnoreCase(String name);

}