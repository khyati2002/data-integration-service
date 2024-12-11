package com.salescode.channelkart.repository;


import com.salescode.channelkart.models.Role;
import java.util.List;

public interface RoleRepository extends CommonJpaRepository<Role, String>{

	List<Role> findByName(String name);

	Role findByNameIgnoreCaseContaining(String name);

	Role findByNameIgnoreCase(String name);

}