package com.example.scientificcalculator.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class CalculationHistoryTest {
    @Test
    void keepsNewestRecordsAndDoesNotExceedCapacity() {
        CalculationHistory history = new CalculationHistory(2);
        history.add("1", AngleUnit.RADIAN, 1);
        history.add("2", AngleUnit.RADIAN, 2);
        history.add("3", AngleUnit.RADIAN, 3);

        assertEquals(List.of("3", "2"),
                history.list(10).stream().map(CalculationRecord::expression).toList());
    }

    @Test
    void clearKeepsIdsMonotonicAndReturnsSnapshot() {
        CalculationHistory history = new CalculationHistory(2);
        long first = history.add("1", AngleUnit.RADIAN, 1).id();
        List<CalculationRecord> snapshot = history.list(10);
        history.clear();

        assertTrue(history.list(10).isEmpty());
        assertEquals(first + 1, history.add("2", AngleUnit.RADIAN, 2).id());
        assertEquals(1, snapshot.size());
    }

    @Test
    void concurrentWritesProduceUniqueIdsWithinCapacity() throws Exception {
        CalculationHistory history = new CalculationHistory(1000);
        int writers = 8;
        int writesPerWriter = 100;
        ExecutorService pool = Executors.newFixedThreadPool(writers);
        CountDownLatch ready = new CountDownLatch(writers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Long>>> futures = new ArrayList<>();
        for (int i = 0; i < writers; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await(2, TimeUnit.SECONDS);
                List<Long> ids = new ArrayList<>();
                for (int j = 0; j < writesPerWriter; j++) {
                    ids.add(history.add("1+1", AngleUnit.RADIAN, 2).id());
                }
                return ids;
            }));
        }
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();
        Set<Long> ids = new HashSet<>();
        for (Future<List<Long>> future : futures) {
            ids.addAll(future.get(5, TimeUnit.SECONDS));
        }
        pool.shutdownNow();

        assertEquals(writers * writesPerWriter, ids.size());
        assertTrue(history.list(1000).size() <= 1000);
    }
}
