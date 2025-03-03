package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.RoleName;
import com.applicate.services.channelkart.repository.UserParentRepository;
import com.salescode.dim.jooq.generated.tables.pojos.UserParent;
import com.salescode.dim.jooq.generated.tables.records.CkUserParentRecord;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_USER_PARENT;

public class UserParentService extends AbstractCDMService<UserParent> {

    private static UserParentRepository userParentRepository;
    private final DSLContext dsl;
    public UserParentService(DSLContext dsl) {
        super(dsl);
        this.dsl = dsl;
        userParentRepository = new UserParentRepository(dsl);
    }

    public void populateAndSaveUserParent(User user){
        if(user.getRoles().size()==1 && user.getRoles().stream().allMatch(desig->desig.getName().equals(RoleName.ROLE_ADMIN.name()))) {
            UserParent up= new UserParent();
            up.setUserloginid(user.getLoginid());
            up.setParent(null);
            up.setLob(user.getLob());
            UserParent refreshedObj=refresh(up);
            save(up,refreshedObj);
        }

        if(user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
           saveUserParent(user);
        }

    }

    public void saveUserParent(User user){
        Set<UserParent> userParents= new HashSet<>();
        List<UserParent> dbParents= findByUserLoginId(user.getLoginid());
        if(dbParents != null && !dbParents.isEmpty()) {
            Set<String> dataset= dbParents.stream().map(UserParent::getParent).collect(Collectors.toSet());
            if(staleRecords(new HashSet<>(dataset), user.getImmediateParent())) {
                userParents.addAll(getNewUserParents(user, dataset));
            }
        }else {
            userParents.addAll(getUserParents(user) );
        }
        if(!userParents.isEmpty()) {
           batchSave(userParents);
        }
    }

    private List<UserParent> getUserParents(User user){
        List<UserParent> userParentList = new ArrayList<>();
        for(HierarchyMetadata hm: user.getImmediateParent()) {
            UserParent up= new UserParent();
            up.setUserloginid(user.getLoginid());
            up.setParent(hm.getParent());
            if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
              //  throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
                  throw new RuntimeException("User can't be mapped to itself");
            }
            userParentList.add(up);
        }
        return userParentList;
    }

    private List<UserParent> getNewUserParents(User user, Set<String> dataset){
        List<UserParent> userParentList = new ArrayList<>();
        for(HierarchyMetadata hm: user.getImmediateParent()) {
            if(!dataset.contains(hm.getParent())) {
                UserParent up= new UserParent();
                up.setUserloginid(user.getLoginid());
                up.setParent(hm.getParent());
                if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
               //     throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
                }
                userParentList.add(up);
            }
        }
        return userParentList;
    }

    public List<UserParent> findByUserLoginId(String loginId) {
        return userParentRepository.findByUserLoginId(loginId);
    }

    private static boolean staleRecords(Set<String> existingParents,List<HierarchyMetadata> immediateParents) {
        Set<String> hmlist = immediateParents.stream().map(HierarchyMetadata::getParent).collect(Collectors.toSet());
        return !existingParents.equals(hmlist);
    }

    private UserParent refresh(UserParent up){
        return dsl.selectFrom(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.eq(up.getUserloginid()))
                .and(CK_USER_PARENT.PARENT.eq(up.getParent()))
                .fetchOneInto(UserParent.class);
    }

    private UserParent save(UserParent up, UserParent savedObj){
        super.addHash(up);
        if(up.getHash() == savedObj.getHash()){
            return up;
        }
        if(savedObj != null) {
            up.setId(savedObj.getId());
            up.setVersion(savedObj.getVersion() + 1);
        }
        else{
            up.setId(UUID.randomUUID().toString());
            up.setVersion(0);
        }
        CkUserParentRecord record = dsl.newRecord(CK_USER_PARENT,up);
        dsl.insertInto(CK_USER_PARENT)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();
        return up;
    }

    public Set<UserParent> batchSave(Set<UserParent> up){
        up.forEach(userParent -> {
            userParent.setId(UUID.randomUUID().toString());
            userParent.setVersion(0);
            super.addHash(userParent);
        });
        dsl.batchInsert(
                up.stream()
                        .map(userParent -> dsl.newRecord(CK_USER_PARENT, userParent))
                        .collect(Collectors.toList())
        ).execute();

        return up;
    }

    @Override
    public UserParent save(UserParent cdmObject) {
        return null;
    }
}
