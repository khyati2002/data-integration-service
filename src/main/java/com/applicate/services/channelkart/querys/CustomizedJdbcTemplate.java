package com.applicate.services.channelkart.querys;

import com.applicate.services.channelkart.services.NativeRowMapper;
import com.applicate.services.channelkart.utils.StringUtils;
import org.hibernate.QueryException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : Jinu
 * Date    : 6/17/2021
 **/
public class CustomizedJdbcTemplate extends JdbcTemplate {

   private final NativeRowMapper rowMapper = new NativeRowMapper();

   public CustomizedJdbcTemplate() {
      super();
   }

   public CustomizedJdbcTemplate(DataSource dataSource) {
      super(dataSource);
   }

   public CustomizedJdbcTemplate(DataSource dataSource, boolean lazyInit) {
      super(dataSource, lazyInit);
   }

   @Override
   protected RowMapper<Map<String, Object>> getColumnMapRowMapper() {
      return this.rowMapper;
   }
   
	@SuppressWarnings("unchecked")
	public <T> List<T> queryNamedParameter(String sqlQuery, Map<String, Object> params, RowMapper<T> rowMapper) throws DataAccessException {
		ConnectionCallback<Object> connectionCallback = new ConnectionCallback<Object>() {
		    public Object doInConnection(Connection connection) throws SQLException {
		    	connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		    	String sql = sqlQuery;
				List<Object> arg = new ArrayList<>();

				for (Map.Entry<String, Object> entry : params.entrySet()) {
					String key = entry.getKey();
					if (StringUtils.isNotEmpty(key) && sql.contains(":"  + key)) {
						sql = sql.replace(":" + key, "?");
						arg.add(entry.getValue());
					}
				}

		      try (PreparedStatement statement = connection.prepareStatement(sql)) {
		    	  int i=0;
		    	  for(Object a:arg) {
		    		  setParameters(statement, a, ++i);
		    	  }
		        statement.execute();
		        try(ResultSet resultSet = statement.getResultSet()) {
		        	List<T> data = new ArrayList<> ();
		        	int j=0;
		        	while (resultSet != null && resultSet.next()) {
		        		data.add(rowMapper.mapRow(resultSet, j++));
		        	}
		        	return data;
		        }
		      } catch (Exception e) {
				throw new QueryException(e.getMessage());
			}
		    }
		  };
		return (List<T>) super.execute(connectionCallback);
	}
	
	private void setParameters(PreparedStatement ps, Object td, int i)
		      throws SQLException {
		    if (td != null) {
		      if (td.equals(Boolean.class) || td.getClass().equals(boolean.class)) {
		        ps.setBoolean(i, Boolean.valueOf(td.toString()));
		      } else {
		        ps.setString(i, td.toString());
		      }
		    } else {
		      ps.setObject(i, null);
		    }
		  }
}
