package com.applicate.services.channelkart.registry;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface Registry<T> {

    void add(T t);

    void addAll(String lob, List<T> t);

    void loadAll(Predicate<String> predicate, boolean overrideOld);

    List<T> get(String lob);

    List<T> get(String lob, String type);

    default List<T> get(String lob, Predicate<T> predicate) {
        return get(lob).stream().filter(t -> predicate.test(t)).collect(Collectors.toList());
    }

    void clear(String lob);

}
