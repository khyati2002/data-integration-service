package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.utils.JdbcUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author : Jinu
 * Date    : 6/17/2021
 **/
@Component
public class NativeRowMapper extends ColumnMapRowMapper {

   @Nullable
   @Override
   protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
      return JdbcUtils.getResultSetValue(rs, index);
   }

}
