package nsu.syspro.task_02_03;


import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;


public class TestMyReentrantLock {
    private final NonReentrantLockFactory factory = new SimpleNonReentrantLockFactory();

    /** Попытка создать MyReentrantLock с null */
    @Test
    public void testConstructorWithNullFactory() {
        try {
            new MyReentrantLock(null);
            fail("Test failed!");
        } catch (IllegalArgumentException e) { /* (>_<) */ }
    }

    /** Проверка реентерабельности */
    @Test
    public void testManyLockAndUnlock() {
        MyReentrantLock lock = new MyReentrantLock(factory);
        int num = 16;
        for (int i = 0; i < num; i++) { lock.lock(); }
        for (int i = 0; i < num; i++) { lock.unlock(); }
    }

    /** Попытка разблокировать из другого потока */
    @Test
    public void testUnlockByOtherThread() throws InterruptedException {
        MyReentrantLock lock = new MyReentrantLock(factory);
        Thread otherThread = new Thread(() -> {
            try {
                lock.unlock();
                fail("Test failed!");
            } catch (IllegalMonitorStateException e) { /* (>_<) */ }
        });

        lock.lock();
        otherThread.start(); otherThread.join();
        lock.unlock();
    }

    /** Попытка разблокировать без предварительного захвата */
    @Test
    public void testUnlockWithoutLock() {
        MyReentrantLock lock = new MyReentrantLock(factory);
        try {
            lock.unlock();
            fail("Test failed!");
        } catch (IllegalMonitorStateException e) { /* (>_<) */ }
    }

    /** Проверяем работоспособность при нескольких работающих потоках */
    @Test
    public void testMultiThreadLock() throws InterruptedException {
        MyReentrantLock lock = new MyReentrantLock(factory);
        AtomicInteger counter = new AtomicInteger(0);
        int numThreads = 16;
        int itersThread = 128;

        Runnable task = () -> {
            for (int i = 0; i < itersThread; i++) {
                try {
                    lock.lock();
                    int current = counter.get();    // critical section
                    Thread.sleep(4);                // critical section
                    counter.set(current + 1);       // critical section
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                  finally { lock.unlock(); }
            }
        };

        Thread[] arr = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) { arr[i] = new Thread(task); arr[i].start(); }
        for (int i = 0; i < numThreads; i++) { arr[i].join(); }

        assertEquals(numThreads * itersThread, counter.get());
    }
}
