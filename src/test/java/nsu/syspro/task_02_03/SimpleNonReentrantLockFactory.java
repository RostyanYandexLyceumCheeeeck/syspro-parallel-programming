package nsu.syspro.task_02_03;

/** Реализация NonReentrantLockFactory */
class SimpleNonReentrantLockFactory implements NonReentrantLockFactory {
    @Override
    public NonReentrantLock create() {
        return new SimpleNonReentrantLock();
    }
}