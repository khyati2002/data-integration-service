package com.applicate.services.channelkart.repository;


import com.applicate.services.channelkart.models.Role;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends CommonJpaRepository<Role, String>{

	List<Role> findByName(String name);

	Role findByNameIgnoreCaseContaining(String name);

	Role findByNameIgnoreCase(String name);

}