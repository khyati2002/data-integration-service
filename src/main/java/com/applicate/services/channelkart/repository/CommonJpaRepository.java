/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package com.applicate.services.channelkart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * The Interface CommonJpaRepository.
 *
 * @author Manish Srivastava
 * @since  Apr 2020
 * @param <T> the generic type
 * @param <E> the element type
 */

@NoRepositoryBean
public interface CommonJpaRepository<T,E extends Serializable> extends JpaRepository<T,E>, JpaSpecificationExecutor<T> {

}
