package com.salescode.channelkart.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class GlobalLock {

  private GlobalLock(){}

  private static final Map<String, Lock> lockMap = new ConcurrentHashMap<>();

  public static void withLock(String key, Consumer<String> s){
    Lock lock = lockMap.computeIfAbsent(key,k->new ReentrantLock());
    lock.lock();
    try{
      s.accept(key);
    }finally {
      lock.unlock();
      lockMap.remove(key);
    }
  }

  public static <T> T withLock1(String key, Function<String,T> s){
    Lock lock = lockMap.computeIfAbsent(key,k->new ReentrantLock());
    lock.lock();
    try{
      return s.apply(key);
    }finally {
      lock.unlock();
      lockMap.remove(key);
    }
  }

}
