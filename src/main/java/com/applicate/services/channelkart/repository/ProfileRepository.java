package com.applicate.services.channelkart.repository;


import com.applicate.services.channelkart.models.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ProfileRepository extends CommonJpaRepository<Profile, String> {

	public List<Profile> findByType(String type);
	
	public List<Profile> findByTypeIn(List<String> type);
	public List<Profile> findByLobAndTypeIn(String lob,List<String> type);
	public List<Profile> findByLobAndType(String log,String type);

	public List<Profile> findByNameAndType(String name,String type);
	
	public Profile findByName(String name);
	
}
