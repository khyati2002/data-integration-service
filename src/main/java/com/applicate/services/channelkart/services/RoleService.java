package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.RoleRepository;
import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import org.jooq.DSLContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RoleService extends AbstractCDMService<AuthRole> {

    private static RoleRepository roleRepository;
    public RoleService(DSLContext dsl) {
        super(dsl);
        roleRepository = new RoleRepository(dsl);
    }

    public List<AuthRole> getRoleAsList(String name) {
        return getRole(name)
                .map(List::of)
                .orElse(Collections.emptyList());
    }

    public Optional<AuthRole> getRole(String name) {
        return Optional.ofNullable(roleRepository.findByNameIgnoreCase(name));
    }

    public List<AuthRole> getRolesByNames(List<String> roleNames) {
        return roleRepository.findByNameIn(roleNames);
    }


    @Override
    public AuthRole save(AuthRole cdmObject) {
        return null;
    }
}
