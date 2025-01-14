package com.salescode.channelkart.event.provider;

import com.salescode.channelkart.event.type.CommonEventProfile;

public class EmbeddedEventProfile extends CommonEventProfile {

   private int maximumThreads;

   public int getMaximumThreads() {
      return maximumThreads;
   }

   public EmbeddedEventProfile setMaximumThreads(int maximumThreads) {
      this.maximumThreads = maximumThreads;
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      EmbeddedEventProfile that = (EmbeddedEventProfile) o;

      if (getMaximumRetry() != that.getMaximumRetry()) return false;
      if (getRetryDelay() != that.getRetryDelay()) return false;
      if (maximumThreads != that.maximumThreads) return false;
      return getProviderType() == that.getProviderType();
   }

   @Override
   public int hashCode() {
      return getProviderType() != null ? getProviderType().hashCode() : 0;
   }

}
