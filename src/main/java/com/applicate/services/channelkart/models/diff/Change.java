package com.applicate.services.channelkart.models.diff;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * created by Siddarth Sreeni on 07-05-2021
 */
@Getter
@Setter
public class Change<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 4139767709103310389L;

    private String name;

    private T current;

    private T previous;

    private ChangeType changeType;

    public Change() {
    }

    public Change(String name, T current, T previous) {
        this.current = current;
        this.previous = previous;
        this.name = name;
        this.changeType = findChangeType(current, previous);
    }

    public Change(String name, T current, T previous, ChangeType changeType) {
        this.name = name;
        this.current = current;
        this.previous = previous;
        this.changeType = changeType;
    }

    protected ChangeType findChangeType(T current, T previous) {
        if (Objects.equals(current, previous)) {
            return ChangeType.NOCHANGE;
        }
        if (current == null) {
            return ChangeType.DELETED;
        }
        if (previous == null) {
            return ChangeType.CREATED;
        }
        // if all the above conditions are failed then the change should be UPDATED
        return ChangeType.UPDATED;
    }

    @Override
    public String toString() {
        return "{field:" + name + ", currentValue:" + current + ", previousValue:" + previous + ", type:" + changeType + '}';
    }
}
