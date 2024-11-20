package com.salescode.dataintegration.etl.cdm.sync.schduler;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author : Jinu
 * Date    : 10/7/2020
 **/
public class UserNameAndContext implements Serializable {

   private static final long serialVersionUID = -8532374820347635164L;

   private final String userName;

   private final String context;

   public UserNameAndContext(String userName, String context) {
      this.userName = userName;
      this.context = context;
   }

   public String getUserName() {
      return userName;
   }

   public String getContext() {
      return context;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      UserNameAndContext that = (UserNameAndContext) o;

      if (!Objects.equals(userName, that.userName)) return false;
      return Objects.equals(context, that.context);
   }

   @Override
   public int hashCode() {
      int result = userName != null ? userName.hashCode() : 0;
      result = 31 * result + (context != null ? context.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "UserNameAndContext{" +
              "userName='" + userName + '\'' +
              ", context='" + context + '\'' +
              '}';
   }
}
