package com.applicate.services.channelkart.services;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.repository.RoleRepository;
import com.applicate.services.channelkart.models.Role;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoleService extends AbstractCDMService<Role> {

	private static final String DOMAIN_NAME = "RoleService";

	private final RoleRepository roleRepository;

	private final DistributedCache distributedCache;

	public RoleService(RoleRepository roleRepository, DistributedCache distributedCache) {
		super(roleRepository);
		this.roleRepository = roleRepository;
		this.distributedCache = distributedCache;
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

	private Role getRoleFromCacheOrRepo(String roleName) {
		String lob = SecurityContextUtils.getLob();
		Role roleFromCache = (Role) distributedCache.get(lob,null, createRoleKey(roleName), false);
		if (roleFromCache == null) {
			Role roleFromRepo = roleRepository.findByNameIgnoreCase(roleName);
			if (roleFromRepo != null) {
				distributedCache.put(lob,null, createRoleKey(roleName), roleFromRepo,false);
				return roleFromRepo;
			}
		}
		return roleFromCache;
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