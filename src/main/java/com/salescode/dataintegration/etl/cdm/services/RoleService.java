package com.salescode.dataintegration.etl.cdm.services;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.repository.RoleRepository;
import com.salescode.jooq.generated.tables.pojos.CkAuthRole;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.salescode.jooq.generated.Tables.CK_AUTH_ROLE;

@Service
public class RoleService extends AbstractCDMService<CkAuthRole> {

	private static final String DOMAIN_NAME = "RoleService";

	private final RoleRepository roleRepository;

	public RoleService(DSLContext context, RoleRepository repository) {
		super(context);
		roleRepository = repository;
	}

	@Override
	protected Table<? extends Record> getTable() {
		return CK_AUTH_ROLE;
	}


	public Optional<CkAuthRole> getRole(String name) {
		return Optional.ofNullable(getRoleFromCacheOrRepo(name));
	}

	public Optional<CkAuthRole> getRole(String name,boolean noCache) {
		return (noCache)? Optional.ofNullable(roleRepository.findByNameIgnoreCase(name)):
				Optional.ofNullable(getRoleFromCacheOrRepo(name));
	}


	private String createRoleKey(String roleName) {
		return DOMAIN_NAME + ":" + roleName.toUpperCase();
	}

	private CkAuthRole getRoleFromCacheOrRepo(String roleName) {
		//String lob = SecurityContextUtils.getLob();
		//CkAuthRole roleFromCache = (CkAuthRole) distributedCache.get(lob,null, createRoleKey(roleName), false);
		//if (roleFromCache == null) {
			CkAuthRole roleFromRepo = roleRepository.findByNameIgnoreCase(roleName);
			if (roleFromRepo != null) {
			//	distributedCache.put(lob,null, createRoleKey(roleName), roleFromRepo,false);
				return roleFromRepo;
		//	}
		}
		//return roleFromCache;
        return roleFromRepo;
	}

	public List<CkAuthRole> getRoleAsList(String name) {
		return getRole(name)
				.map(List::of)
				.orElse(Collections.emptyList());
	}

	@Override
	public CkAuthRole refresh(CkAuthRole role) {
		CkAuthRole existingRole = roleRepository.findByNameIgnoreCase(role.getName());
		return existingRole == null ? role : existingRole;
	}

	@Override
	public CkAuthRole save(CkAuthRole role) {
		CkAuthRole refreshedRole = refresh(role);
		return super.save(refreshedRole);
	}

}