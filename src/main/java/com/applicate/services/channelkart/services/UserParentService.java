package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.RoleName;
import com.applicate.services.channelkart.repository.UserParentRepository;
import com.salescode.dim.jooq.generated.tables.pojos.UserParent;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import org.jooq.TableRecord;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_USER_PARENT;

public class UserParentService extends AbstractCDMService<UserParent> {

    private static UserParentRepository userParentRepository;

    public UserParentService(){
        if(userParentRepository == null){
            userParentRepository = new UserParentRepository(getDslContext());
        }
    }
    public void populateAndSaveUserParent(User user){
        if(user.getRoles().size()==1 && user.getRoles().stream().allMatch(desig->desig.getName().equals(RoleName.ROLE_ADMIN.name()))) {
            UserParent up= new UserParent();
            up.setActiveStatus(user.getActiveStatus());
            up.setUserloginid(user.getLoginid());
            up.setParent(null);
            up.setLob(user.getLob());
            up.setChanged(true);
            save(up);
        }

        if(user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
            saveUserParent(user);
        }

    }

    private List<UserParent> getNewUserParents(User user, Set<String> dataset){
        List<UserParent> userParentList = new ArrayList<>();
        for(HierarchyMetadata hm: user.getImmediateParent()) {
            if(!dataset.contains(hm.getParent())) {
                UserParent up= new UserParent();
                up.setUserloginid(user.getLoginid());
                up.setActiveStatus(user.getActiveStatus());
                up.setId(UUID.randomUUID().toString());
                up.setParent(hm.getParent());
                up.setChanged(true);
                if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
             //       throw new RuntimeException("User can't be mapped to itself. Found a record for user  mapped to itself. Please verify the data once.");
                }
                else {
                    userParentList.add(up);
                }
            }
        }
        return userParentList;
    }

    private static boolean staleRecords(Set<String> existingParents,List<HierarchyMetadata> immediateParents) {
        Set<String> hmlist = immediateParents.stream().map(HierarchyMetadata::getParent).collect(Collectors.toSet());
        return !existingParents.equals(hmlist);
    }

    private List<UserParent> getUserParents(User user){
        List<UserParent> userParentList = new ArrayList<>();
        for(HierarchyMetadata hm: user.getImmediateParent()) {
            UserParent up= new UserParent();
            up.setUserloginid(user.getLoginid());
            up.setParent(hm.getParent());
            up.setId(UUID.randomUUID().toString());
            up.setActiveStatus(user.getActiveStatus());
            up.setChanged(true);
            if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
                //  throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
                throw new RuntimeException("User can't be mapped to itself");
            }
            userParentList.add(up);
        }
        return userParentList;
    }

    public void batchSave(Set<UserParent> userParents) {
        userParents.forEach(this::fillCommonAttributes);

        List<TableRecord<?>> records = userParents.stream()
                .map(userParent -> getDslContext().newRecord(CK_USER_PARENT, userParent))
                .collect(Collectors.toList());

        getDslContext().batchInsert(records).execute();
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

    public List<UserParent> findByUserLoginId(String loginId) {
        return userParentRepository.findByUserLoginId(loginId);
    }

}
