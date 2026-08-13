package com.example.scientificcalculator.calculation;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class CalculationHistory {
    private final int capacity;
    private final Deque<CalculationRecord> records = new ArrayDeque<>();
    private long nextId;

    public CalculationHistory(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public CalculationRecord add(String expression, AngleUnit angleUnit, double result) {
        synchronized (records) {
            CalculationRecord record = new CalculationRecord(++nextId, expression, angleUnit, result, Instant.now());
            records.addFirst(record);
            while (records.size() > capacity) {
                records.removeLast();
            }
            return record;
        }
    }

    public List<CalculationRecord> list(int limit) {
        synchronized (records) {
            return List.copyOf(new ArrayList<>(records).subList(0, Math.min(limit, records.size())));
        }
    }

    public Optional<CalculationRecord> find(long id) {
        synchronized (records) {
            return records.stream().filter(record -> record.id() == id).findFirst();
        }
    }

    public void clear() {
        synchronized (records) {
            records.clear();
        }
    }
}
