package nsu.syspro.task_02_03;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** Реализация NonReentrantLock для тестов */
class SimpleNonReentrantLock implements NonReentrantLock {
    private final Lock lock = new ReentrantLock();
    private volatile Thread owner = null;

    @Override
    public void lock() {
        lock.lock();

        Thread currentThread = Thread.currentThread();
        if (owner == currentThread) {
            lock.unlock();
            throw new IllegalMonitorStateException("Поток уже удерживает NonReentrantLock!");
        }
        owner = currentThread;
    }

    @Override
    public void unlock() {
        Thread currentThread = Thread.currentThread();
        if (owner != currentThread) {
            throw new IllegalMonitorStateException("Не этот поток удерживает NonReentrantLock!");
        }

        owner = null;
        lock.unlock();
    }
}
