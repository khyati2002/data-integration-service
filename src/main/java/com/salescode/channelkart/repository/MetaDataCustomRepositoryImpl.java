package com.salescode.channelkart.repository;


import com.salescode.channelkart.models.MetaData;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import java.util.List;
import java.util.NoSuchElementException;

@Repository
@Primary
public class MetaDataCustomRepositoryImpl implements MetaDataCustomRepository
{

	private static final String DOMAIN_NAME = "domainName";
	@PersistenceContext
	EntityManager entityManager;

	@Override
	public List<MetaData> findAll(String domainName) {
		 StringBuilder queryStr = new StringBuilder("select m from MetaData m WHERE m.domainName=:domainName");
		 Query query = entityManager.createQuery(queryStr.toString(), MetaData.class); 
		 query.setParameter(DOMAIN_NAME, domainName);
		return query.getResultList();
	}

	@Override
	public MetaData findByValue(String domainName,String domainType) {
		StringBuilder queryStr = new StringBuilder("select m from MetaData m WHERE m.domainName =:domainName AND  m.domainType=:domainType");
		 Query query = entityManager.createQuery(queryStr.toString(), MetaData.class); 
		 query.setParameter(DOMAIN_NAME, domainName).setParameter("domainType", domainType);
		return (MetaData) query.getResultList().stream().findFirst().orElse(null);
	}


	@Override
	public MetaData merge(MetaData metaData) {
		return entityManager.merge(metaData);
	}


	@Override
	@Transactional
	public MetaData deleteByValue(String domainName, String domainType) throws NoSuchElementException {
		MetaData metaData = this.findByValue(domainName, domainType);
		if(metaData == null) {
			throw new NoSuchElementException("Metadata not found");
		}
		this.delete(metaData);
		return metaData;
	}

	@Transactional
	public void delete(MetaData entity) {
		this.entityManager.remove(entity);
	}

	@Override
	public List<MetaData> findByDomainType(String domainType) {
		StringBuilder queryStr = new StringBuilder("select m from MetaData m WHERE m.domainType=:domainType");
		Query query = entityManager.createQuery(queryStr.toString(), MetaData.class); 
		query.setParameter("domainType", domainType);
		return query.getResultList();
	}

	@Override
	public List<MetaData> findByDomainName(String domainName) {
		String query = "select m from MetaData m where m.domainName=:domainName";
		Query queryEntity = entityManager.createQuery(query, MetaData.class);
		queryEntity.setParameter(DOMAIN_NAME, domainName);
		return queryEntity.getResultList();
	}

}
