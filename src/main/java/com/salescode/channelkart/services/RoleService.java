package com.salescode.channelkart.services;
import com.salescode.channelkart.repository.RoleRepository;
import com.salescode.channelkart.models.Role;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class RoleService extends AbstractCDMService<Role> {

	private static final String DOMAIN_NAME = "RoleService";

	private final RoleRepository roleRepository;

	public RoleService(RoleRepository roleRepository) {
		super(roleRepository);
		this.roleRepository = roleRepository;
	}


	public Optional<Role> getRole(String name) {
		return Optional.ofNullable(getRoleFromCacheOrRepo(name));
	}

	public Optional<Role> getRole(String name,boolean noCache) {
		return (noCache)? Optional.ofNullable(roleRepository.findByNameIgnoreCase(name)):
				Optional.ofNullable(getRoleFromCacheOrRepo(name));
	}


	private String createRoleKey(String roleName) {
		return DOMAIN_NAME + ":" + roleName.toUpperCase();
	}

	Map<String,Role> map = new ConcurrentHashMap<>();
	private Role getRoleFromCacheOrRepo(String roleName) {
		//String lob = SecurityContextUtils.getLob();
		//Role roleFromCache = (Role) distributedCache.get(lob,null, createRoleKey(roleName), false);
		//if (roleFromCache == null) {

			Function function =	(role)-> getByNameIgnoreCase(roleName);
			Role roleFromRepo = map.computeIfAbsent(roleName, function);
			if (roleFromRepo != null) {
			//	distributedCache.put(lob,null, createRoleKey(roleName), roleFromRepo,false);
				return roleFromRepo;
		//	}
		}
		//return roleFromCache;
        return roleFromRepo;
	}

	private Role getByNameIgnoreCase(String roleName) {
		return roleRepository.findByNameIgnoreCase(roleName);
	}

	public List<Role> getRoleAsList(String name) {
		return getRole(name)
				.map(List::of)
				.orElse(Collections.emptyList());
	}

	@Override
	public Role refresh(Role role) {
		Role existingRole = roleRepository.findByNameIgnoreCase(role.getName());
		return existingRole == null ? role : existingRole;
	}

	@Override
	public Role save(Role role) {
		Role refreshedRole = refresh(role);
		return super.save(refreshedRole);
	}

}