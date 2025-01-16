package com.applicate.services.channelkart.utils;

public class DataPair<K,V> {
    private K k;
    private V v;
    public DataPair(K k, V v){
        this.k=k;
        this.v=v;
    }

    public K getK() {
        return k;
    }

    public V getV() {
        return v;
    }

    @Override
    public String toString() {
        return "DataPair{" +
                "key=" + k +
                ", value=" + v +
                '}';
    }
}
