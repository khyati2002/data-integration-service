package com.salescode.channelkart.repository;



import com.salescode.channelkart.models.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ProfileRepository extends CommonJpaRepository<Profile, String> {

	public List<Profile> findByLobAndType(String log,String type);

}
