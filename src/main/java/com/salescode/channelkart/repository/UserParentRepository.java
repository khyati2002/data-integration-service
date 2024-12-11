package com.salescode.channelkart.repository;


import com.salescode.channelkart.models.UserParent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The Interface UserParentRepository.
 * 
 * @author Manish Srivastava
 * @since  Jun 2020
 */
@Repository
public interface UserParentRepository extends CommonJpaRepository<UserParent, String> {

    /**
     * Find by user login id.
     *
     * @param loginId the login id
     * @return the list
     */
    public List<UserParent> findByUserLoginId(String loginId);
    
    /**
     * Find by user login id and parent.
     *
     * @param loginid the loginid
     * @param parentloginid the parentloginid
     * @return the user parent
     */
    public UserParent findByUserLoginIdAndParent(String loginid,String parentloginid);
	
    /**
     * Delete by user login id.
     *
     * @param loginid the loginid
     */
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query(value="Delete From UserParent up Where up.userLoginId = ?1")
    public void deleteByUserLoginId(String loginid);
    
    /**
     * Delete by user login id in.
     *
     * @param loginid the loginid
     */
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    public void deleteByUserLoginIdIn(Collection<String> loginid);
    

    List<UserParent> findByParentIn(List<String> parents);

    @Query(nativeQuery = true,value = "SELECT userloginid, parent FROM ck_user_parent WHERE userloginid IN (?1)")
    List<Map<String, Object>> getUserParentMapping(List<String> outletList);
}

