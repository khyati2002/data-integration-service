package com.salescode.channelkart.batch.repo;

/**
 * @author : Jinu
 * Date    : 11/3/2020
 **/
public class HashAwareData {

   private String id;

   private int version;

   private String hash;

   public HashAwareData(String id, int version, String hash) {
      this.id = id;
      this.version = version;
      this.hash = hash;
   }

   public String getId() {
      return id;
   }

   public HashAwareData setId(String id) {
      this.id = id;
      return this;
   }

   public int getVersion() {
      return version;
   }

   public HashAwareData setVersion(int version) {
      this.version = version;
      return this;
   }

   public String getHash() {
      return hash;
   }

   public HashAwareData setHash(String hash) {
      this.hash = hash;
      return this;
   }
}
