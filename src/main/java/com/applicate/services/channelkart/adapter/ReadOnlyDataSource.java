package com.applicate.services.channelkart.adapter;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Logger;

/**
 * @author : Jinu
 * Date    : 4/16/2021
 **/
public class ReadOnlyDataSource implements DataSource {

   private final DataSource dataSource;

   public ReadOnlyDataSource(DataSource dataSource) {
      this.dataSource = dataSource;
   }

   @Override
   public Connection getConnection() throws SQLException {
      return new ReadOnlyConnection(dataSource.getConnection());
   }

   @Override
   public Connection getConnection(String username, String password) throws SQLException {
      return dataSource.getConnection(username, password);
   }

   @Override
   public PrintWriter getLogWriter() throws SQLException {
      return dataSource.getLogWriter();
   }

   @Override
   public void setLogWriter(PrintWriter out) throws SQLException {
      dataSource.setLogWriter(out);
   }

   @Override
   public void setLoginTimeout(int seconds) throws SQLException {
      dataSource.setLoginTimeout(seconds);
   }

   @Override
   public int getLoginTimeout() throws SQLException {
      return dataSource.getLoginTimeout();
   }

   @Override
   public ConnectionBuilder createConnectionBuilder() throws SQLException {
      return dataSource.createConnectionBuilder();
   }

   @Override
   public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return dataSource.getParentLogger();
   }

   @Override
   public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
      return dataSource.createShardingKeyBuilder();
   }

   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException {
      return dataSource.unwrap(iface);
   }

   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return dataSource.isWrapperFor(iface);
   }
}
