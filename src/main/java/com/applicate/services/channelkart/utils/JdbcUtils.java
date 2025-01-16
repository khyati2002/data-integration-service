package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.applicate.services.channelkart.adapter.ReadOnlyDataSource;
import com.applicate.services.channelkart.querys.CustomizedJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * @author : Jinu
 * Date    : 1/20/2021
 **/
public class JdbcUtils {
	

   private JdbcUtils() {
      throw new UnsupportedOperationException("Utility class");
   }

   public static JdbcTemplate createJdbcTemplate(DataSource dataSource) {
      return new CustomizedJdbcTemplate(dataSource);
   }

   public static JdbcTemplate createReadableJdbcTemplate(String lob) {
      return new CustomizedJdbcTemplate(getReadOnlyDataSource(lob));
   }
   
   public static JdbcTemplate createWritableTemplate(String lob) {
      return new CustomizedJdbcTemplate(getDataSource(lob));
   }

   public static NamedParameterJdbcTemplate createNamedJdbcTemplate(String lob) {
      return new NamedParameterJdbcTemplate(getReadOnlyDataSource(lob));
   }
   
   public static JdbcTemplate createReadableJdbcTemplate(DataSource dataSource) {
      return new CustomizedJdbcTemplate(toReadOnlyDataSource(dataSource));
   }

   public static NamedParameterJdbcTemplate createNamedJdbcTemplate(DataSource dataSource) {
      return new NamedParameterJdbcTemplate(toReadOnlyDataSource(dataSource));
   }

   public static NamedParameterJdbcTemplate createWritableNamedJdbcTemplate(DataSource dataSource) {
      return new NamedParameterJdbcTemplate(dataSource);
   }

   public static NamedParameterJdbcTemplate createWritableNamedJdbcTemplate(String lob) {
      return new NamedParameterJdbcTemplate(getDataSource(lob));
   }
   
   private static DataSource getReadOnlyDataSource(String lob) {
      return toReadOnlyDataSource(getDataSource(lob));
   }
   
   private static DataSource getDataSource(String lob) {
      return (DataSource) DatabaseProfileRegistry.getDataSourceHashMap().get(lob);
   }
   
   public static DataSource toReadOnlyDataSource(DataSource source) {
      return new ReadOnlyDataSource(source);
   }

   private static String getClassName(Object obj) {
      if (obj != null) {
         return obj.getClass().getName();
      }
      return null;
   }

   private static byte[] readBlob(Blob blob) throws SQLException {
      return blob.getBytes(1L, (int) blob.length());
   }

   private static String readClob(Clob clob) throws SQLException {
      return clob.getSubString(1L, (int)clob.length());
   }

   @Nullable
   public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {
      var obj = rs.getObject(index);
      String className = getClassName(obj);
      if (obj instanceof Blob) {
         obj = readBlob((Blob) obj);
      } else if (obj instanceof Clob) {
         obj = readClob((Clob) obj);
      } else if (!"oracle.sql.TIMESTAMP".equals(className) && !"oracle.sql.TIMESTAMPTZ".equals(className)) {
         obj = getTimeStamp(className, index, rs, obj);
      } else {
         obj = rs.getTimestamp(index);
      }
      return obj;
   }

   private static Object getTimeStamp(String className, int index, ResultSet rs, Object obj) throws SQLException {
      if (className != null && className.startsWith("oracle.sql.DATE")) {
         String metaDataClassName = rs.getMetaData().getColumnClassName(index);
         if (!"java.sql.Timestamp".equals(metaDataClassName) && !"oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
            return rs.getDate(index);
         } else {
            return rs.getTimestamp(index);
         }
      } else if (( obj instanceof LocalDateTime) || (obj instanceof Date && "java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index)))) {
         return rs.getTimestamp(index);
      }
      return obj;
   }
   
   
}
