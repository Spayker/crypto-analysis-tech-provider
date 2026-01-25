package com.spayker.crypto.analysis.dto.indicator;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FixedDataList<T> {

    @Getter
    private final String name;
    private final int maxSize;
    private final Deque<T> deque;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public FixedDataList(String name, int maxSize, Collection<? extends T> data) {
        this.name = name;
        this.maxSize = maxSize;
        this.deque = new LinkedList<>(data);
    }

    public void add(T item) {
        lock.writeLock().lock();
        try {
            deque.addLast(item);
            if (deque.size() > maxSize) {
                deque.removeFirst();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public T getLast() {
        lock.readLock().lock();
        try {
            return deque.peekLast();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void replaceLast(T item) {
        lock.writeLock().lock();
        try {
            if (!deque.isEmpty()) {
                deque.removeLast();
            }
            deque.addLast(item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<T> snapshot() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(deque);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addAll(List<T> history) {
        lock.writeLock().lock();
        try {
            for (T item : history) {
                deque.addLast(item);
                if (deque.size() > maxSize) {
                    deque.removeFirst();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getSize() {
        return deque.size();
    }
}
