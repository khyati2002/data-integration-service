package com.salescode.channelkart.services;

import com.salescode.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.CustomerAccountInfo;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.EntityInfo;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class NativeEntityManager<T extends CommonDataModel> {

  public static final String STACKTRACE = "stacktrace";
  @Autowired
  EntityUtils entityUtils;

  private static Logger logger = LoggerFactory.getLogger(NativeEntityManager.class);

  public List<T> create(List<T> objects) {
    JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate(
        (DataSource) DatabaseProfileRegistry.getDataSourceHashMap().get(
            SecurityContextUtils.getLob()));
    batchInsert(jdbcTemplate, objects, 1000);
    return objects;
  }

  public List<T> create(List<T> objects,String onDupUpdate) {
    String lob = SecurityContextUtils.getLob();
    if(lob==null){
      lob="default";
    }
    JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate(
        (DataSource) DatabaseProfileRegistry.getDataSourceHashMap().get(lob));
    batchInsert(jdbcTemplate, objects, 1000,onDupUpdate);
    return objects;
  }

  public List<T> update(List<T> objects) {
   return update(objects,null);
  }

  public List<T> update(List<T> objects,List<String> fieldNames) {
    JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate(
        (DataSource) DatabaseProfileRegistry.getDataSourceHashMap().get(
            SecurityContextUtils.getLob()));
    long start=System.currentTimeMillis();
    jdbcTemplate.batchUpdate(batchUpdateSQL(objects,fieldNames).toArray(new String[0]));
    logger.info("Time taken to execute native sql update ->"+(System.currentTimeMillis()-start));
    return objects;
  }

  public long batchInsert(JdbcTemplate jdbcTemplate, List<T> objects, int batchSize) {

    return batchInsert(jdbcTemplate,objects,batchSize,null);
  }

  public long batchInsert(JdbcTemplate jdbcTemplate, List<T> objects, int batchSize,String onDupUpdate) {
    EntityInfo einfo = entityUtils.getEntityInfo(objects.get(0).getClass());
    Map<String, String> dataList = getDBFields(einfo);
    List<String> extraSQLs = new ArrayList<>();
    String sql = "insert into " + einfo.getTableName() + " (" + String
        .join(",", dataList.values()) + ") values( " + createParamHolder(
        dataList.size()) + " )";
    if(onDupUpdate!=null){
      sql +=" "+ onDupUpdate;
    }
    int[][] updateCounts = jdbcTemplate.batchUpdate(
        sql,
        objects,
        batchSize,
        (PreparedStatement ps, T argument) -> {
          AtomicInteger i = new AtomicInteger(0);
          Map<String, TypedData> dataMap = toDatamap(einfo, argument,null);
          dataList.forEach((k, v) -> {
            try {
              TypedData td = dataMap.get(k);
              setParameters(ps, td, i);
              extraSQLs.addAll(getExtraSQLS(einfo,dataMap));
            } catch (Exception e) {
              logger.error(STACKTRACE, e);
            }

          });
        });
    if(extraSQLs.size()>0){
      jdbcTemplate.batchUpdate(extraSQLs.toArray(new String[0]));
    }
    return Arrays.stream(updateCounts).flatMap(f -> Stream.of(f)).count();
  }

  public List<String> batchUpdateSQL(List<T> objects,List<String> fieldNames) {
    List<String> extraSQLs = new ArrayList<>();
    List<String> sqls= objects.stream().map(o -> {
      EntityInfo einfo = entityUtils.getEntityInfo(o.getClass());
      Map<String, String> dataList = getDBFields(einfo);
      Map<String, TypedData> dataMap = toDatamap(einfo, o,fieldNames);
      List<Entry<String, String>> entries = dataList.entrySet().stream()
          .filter(k -> !"id".equals(k.getKey()) && dataMap.containsKey(k.getKey()))
          .collect(Collectors.toList());
      extraSQLs.addAll(getExtraSQLS(einfo,dataMap));
      return "update " + einfo.getTableName() + " set " + getSetList(entries, dataMap)
          + " where id=" + escape(dataMap.get("id"));
    }).collect(Collectors.toList());
    List<String> finalList = new ArrayList<>();
    finalList.addAll(sqls);
    finalList.addAll(extraSQLs);

    logger.info("SQL List to Execute ->"+finalList);
    return finalList;
  }

  private String getSetList(List<Entry<String, String>> keys, Map<String, TypedData> dataMap) {
    return keys.stream().map(k -> " " + k.getValue() + "=" + setParameters(dataMap.get(k.getKey())))
        .collect(Collectors.joining(","));
  }

  private String escape(TypedData o) {
    return "'" + o.getValue() + "'";
  }


  private String escape(String o) {
    return "'" + o.replace("'", "''") + "'";
  }

  private void setParameters(PreparedStatement ps, TypedData td, AtomicInteger i)
      throws SQLException, ParseException {
    if (td != null) {
      if (td.getType().equals(Boolean.class) || td.getType().equals(boolean.class)) {
        ps.setBoolean(i.incrementAndGet(), Boolean.valueOf(td.getValue().toString()));
      } else if (td.getType().equals(Date.class)) {
      	ps.setString(i.incrementAndGet(), clientToUtcDateConvert((Date)td.getValue()));
      } else {
        ps.setString(i.incrementAndGet(), td.getValue().toString());
      }
    } else {
      ps.setObject(i.incrementAndGet(), null);
    }
  }

  private String setParameters(TypedData td) {
    try {
      if (td != null) {
        if (td.getType().equals(Boolean.class) || td.getType().equals(boolean.class)) {
          return td.getValue().toString();
        } else if (td.getType().equals(Date.class)) {
        	return escape(clientToUtcDateConvert((Date)td.getValue()));
        }else {
          return escape(td.getValue().toString());
        }
      } else {
        return null;
      }
    } catch (Exception e) {
      logger.error(STACKTRACE, e);
      return null;
    }
  }

  private Object getRefrencedId(JoinColumn jc,CommonDataModel cd){
    if(jc==null || jc.referencedColumnName().isEmpty()){
      return cd.getId();
    }else{
      String refColumn = jc.referencedColumnName();
      EntityInfo einfo = entityUtils.getEntityInfo(cd.getClass());
      List<Entry<String,String>> s = einfo.getFieldNameMap().entrySet().stream().filter(e->e.getKey().equalsIgnoreCase(refColumn)).collect(
          Collectors.toList());
      if(s.size()==0){
        s = einfo.getFieldNameMap().entrySet().stream().filter(e->e.getValue().equalsIgnoreCase(refColumn)).collect(
            Collectors.toList());
      }
      if(s.size()>0){
        Field f = getField(cd.getClass(),s.get(0).getKey());
        try {
          return f.get(cd);
        } catch (IllegalAccessException e) {
          logger.error(STACKTRACE, e);
        }
        return null;
      }else{
        return null;
      }
    }
  }

  private Map<String, TypedData> toDatamap(EntityInfo einfo, CommonDataModel cdm,List<String> fieldNames) {
    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    einfo.getFieldNameMap().forEach((k, v) -> {
      try {
        Field f =null;
        if(fieldNames==null || fieldNames.contains(k)){
          f= getField(cdm.getClass(), k);
        }
        if (f != null) {
          f.setAccessible(true);
          Object o = f.get(cdm);
          if (o != null) {
            if(o instanceof CommonDataModel) {
              JoinColumn jc =f.isAnnotationPresent(JoinColumn.class)? f.getAnnotation(JoinColumn.class):null;
              TypedData td = new TypedData(f.getType(), getRefrencedId(jc, (CommonDataModel) o));
              dataMap.put(k, td);
              td.setDbField(v);
            }else{
              TypedData td = new TypedData(f.getType(), o);
              dataMap.put(k, td);
              td.setDbField(v);
            }
          }
        }
      } catch (Exception e) {
        logger.error(STACKTRACE, e);
      }
    });
    return dataMap;
  }

  private String constructInsert(String tableName, List<String> keys) {
    return "insert into " + tableName + " (" + String.join(",", keys) + ") values("
        + createParamHolder(keys.size()) + ")";
  }

  private String constructInsert(String tableName, List<String> keys, List<String> values) {
    values = values.stream().map(v -> "'" + v + "'").collect(Collectors.toList());
    return "insert into " + tableName + " (" + String.join(",", keys) + ") values(" + String
        .join(",", values) + ")";
  }

  public Map<String, String> getDBFields(EntityInfo einfo) {
    try {
      Class c = Class.forName(einfo.getClassName());
      Map<String, String> dataList = new LinkedHashMap<>();
      einfo.getFieldNameMap().forEach((k, v) -> {
        try {
          Field f = getField(c, k);
          if (f != null && !(f.isAnnotationPresent(ManyToMany.class) || f.isAnnotationPresent(
              CollectionTable.class))) {
            dataList.put(k, v);
          }
        } catch (Exception e) {
          logger.error(STACKTRACE, e);
        }
      });
      return dataList;
    } catch (Exception e) {
      logger.error(STACKTRACE, e);
    }
    return null;
  }

  private List<String> getExtraSQLS(EntityInfo einfo,Map<String, TypedData> dataMap) {
    try {
      Class c = Class.forName(einfo.getClassName());
      List<String> dataList = new ArrayList<>();
      einfo.getFieldNameMap().forEach((k, v) -> {
        try {
          Field f = getField(c, k);
          if (f != null) {
              if(f.isAnnotationPresent(ManyToMany.class)){
                dataList.addAll(getManytoManySQLs(einfo,f,k,dataMap));
              }else if(f.isAnnotationPresent(
                  CollectionTable.class)) {
                dataList.addAll(getCollectionTableSQLs(einfo,f,k,dataMap));
              }
          }
        } catch (Exception e) {
          logger.error(STACKTRACE, e);
        }
      });
      return dataList;
    } catch (Exception e) {
      logger.error(STACKTRACE, e);
    }
    return null;
  }

  private List<String> getManytoManySQLs(EntityInfo einfo,Field f,String key,Map<String, TypedData> dataMap){
    List<String> dataList = new ArrayList<>();
    JoinTable j = f.getAnnotation(JoinTable.class);
    String tableName = j.name();
    String joinColumn = j.joinColumns()[0].name();
    String joinColumnValue = dataMap.get("id").value.toString();
    String invjoinColumn = j.inverseJoinColumns()[0].name();
    TypedData td =dataMap.get(key);
    if(td!=null) {
      List<CommonDataModel> cdms = (List<CommonDataModel>) td.getValue();
      if (cdms!=null && cdms.size() > 0) {
        String deleteSQL =
            "delete from " + tableName + " where " + joinColumn + "=" + escape(joinColumnValue);
        dataList.add(deleteSQL);
        cdms.forEach(c -> {
          String sql =
              "insert into " + tableName + " (" + joinColumn + "," + invjoinColumn + " ) values("
                  + escape(joinColumnValue) + "," + escape(c.getId()) + ")";
          dataList.add(sql);
        });
      }
    }
    return dataList;
  }

  private List<String> getCollectionTableSQLs(EntityInfo einfo,Field f,String key,Map<String, TypedData> dataMap){
    CollectionTable ct = f.getAnnotation(CollectionTable.class);
    List<String> dataList = new ArrayList<>();
    String refCol = ct.joinColumns()[0].referencedColumnName();
    String name = ct.joinColumns()[0].name();
    String fieldName = f.getAnnotation(Column.class).name();
    String tableName = einfo.getTableName() + fieldName;
    TypedData td = dataMap.get(refCol);
    TypedData tdField = dataMap.get(key);
    if(tdField!=null) {
      Collection<String> cVal = (Collection<String>) tdField.getValue();
      if (cVal != null ) {
        String deleteSQL = "delete from " + tableName + " where " + td.getDbField() + "=" + escape(
            td.getValue().toString());
        dataList.add(deleteSQL);
        for (String s : cVal) {
          String sql =
              "insert into " + tableName + " (" + td.getDbField() + "," + fieldName
                  + ") values(" + escape(td.getValue().toString()) + "," + escape(s)
                  + ")";
          dataList.add(sql);
        }
      }
    }
    return dataList;
  }

  public static List<Map<String, Object>> query(String query){
    JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate(
        (DataSource) DatabaseProfileRegistry.getDataSourceHashMap()
            .get(SecurityContextUtils.getLob()));
    List<Map<String, Object>> dataOut = null;
    return jdbcTemplate.queryForList(query);
  }

  public static List<Map<String, Object>> paginatedQuery(String query,int limit,int offset){
    List<Map<String, Object>> finalOutlist = new ArrayList<>();
    List<Map<String, Object>> out=null;
    do {
      String fquery = String.format(query, new Object[]{limit, offset});
      out = query(fquery);
      finalOutlist.addAll(out);
    } while (out != null && out.size() >= limit);
    return finalOutlist;
  }

  private Field getField(Class c, String field) {
    try {
      Field f = c.getDeclaredField(field);
      f.setAccessible(true);
      return f;
    } catch (Exception e) {
      Class sc = c.getSuperclass();
      if (sc.equals(Object.class)) {
        return null;
      } else {
        return getField(sc, field);
      }
    }
  }


  public String createParamHolder(int length) {
    StringJoiner joiner = new StringJoiner(",");
    for (int i = 0; i < length; i++) {
      joiner.add("?");
    }
    return joiner.toString();
  }

  private static class TypedData {
    private Object value;
    private Class type;
    private String dbField;
    public TypedData(Class type, Object value) {
      this.type = type;
      this.value = value;
    }
    public String getDbField() {
      return dbField;
    }
    public void setDbField(String dbField) {
      this.dbField = dbField;
    }
    public Object getValue() {
      return value;
    }
    public void setValue(Object value) {
      this.value = value;
    }
    public Class getType() {
      return type;
    }
    public void setType(Class type) {
      this.type = type;
    }
  }

  private String clientToUtcDateConvert(Date date) throws ParseException {
	  Calendar cal=Calendar.getInstance(getClientTimeZone());
	  cal.setTime(date);
	  SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	  sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	  return sdf.format(cal.getTime());
  }
  
  private TimeZone getClientTimeZone() {
		String lob = SecurityContextUtils.getLob();
		CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator
				.lookup(CustomerAccountInfo.class);
		String timeZoneStr = customerService.getTimeZone(lob);
		timeZoneStr = timeZoneStr == null ? "IST" : timeZoneStr;
		return TimeZone.getTimeZone(timeZoneStr);
	}
}