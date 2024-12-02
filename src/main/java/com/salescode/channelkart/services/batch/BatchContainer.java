package com.applicate.services.channelkart.batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections.ListUtils;

/**
 * @author : Jinu
 * Date    : 11/5/2020
 **/
public class BatchContainer<T> {

    private Collection<T> elementsToInsert;

    private Collection<T> elementsToUpdate;

    private Collection<T> duplicateElements;

    public Collection<T> getElementsToInsert() {
        if (this.elementsToInsert == null) {
            this.elementsToInsert = new ArrayList<>();
        }
        return elementsToInsert;
    }

    public BatchContainer<T> setElementsToInsert(Collection<T> elementsToInsert) {
        getElementsToInsert().addAll(elementsToInsert);
        return this;
    }

    public Collection<T> getDuplicateElements() {
        if (this.duplicateElements == null) {
            this.duplicateElements = new ArrayList<>();
        }
        return duplicateElements;
    }

    public BatchContainer<T> setDuplicateElements(Collection<T> duplicateElements) {
        this.duplicateElements = duplicateElements;
        return this;
    }

    public BatchContainer<T> addToInsert(T element) {
        getElementsToInsert().add(element);
        return this;
    }

    public BatchContainer<T> addToDuplicate(T element) {
        getDuplicateElements().add(element);
        return this;
    }

    public List<T> getElementsToInsertAsList() {
        if (this.elementsToInsert instanceof List) {
            return (List<T>) this.elementsToInsert;
        }
        return new ArrayList<>(this.getElementsToInsert());
    }

    public List<T> getDuplicateElementsAsList() {
        if (this.duplicateElements instanceof List) {
            return (List<T>) this.duplicateElements;
        }
        return new ArrayList<>(this.getDuplicateElements());
    }

    public Collection<T> getElementsToUpdate() {
        if (this.elementsToUpdate == null) {
            this.elementsToUpdate = new ArrayList<>();
        }
        return elementsToUpdate;
    }

    public List<T> getElementsToUpdateAsList() {
        if (this.elementsToUpdate instanceof List) {
            return (List<T>) this.elementsToUpdate;
        }
        return new ArrayList<>(this.getElementsToUpdate());
    }

    public BatchContainer<T> setElementsToUpdate(Collection<T> elementsToUpdate) {
        getElementsToUpdate().addAll(elementsToUpdate);
        return this;
    }

    public BatchContainer<T> addToUpdate(T element) {
        getElementsToUpdate().add(element);
        return this;
    }

    public List<T> getAllElementstoSave(){
        return ListUtils.union(this.getElementsToInsertAsList(), this.getElementsToUpdateAsList());
    }
}
