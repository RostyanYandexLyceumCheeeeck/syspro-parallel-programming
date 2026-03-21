package nsu.syspro.task_02_03;



interface NonReentrantLock {
    void lock();
    void unlock();
}

interface NonReentrantLockFactory {
    NonReentrantLock create();
}
/**
 * Reentrant мьютекс, созданный поверх NonReentrantLockFactory.
 *
 * <p>Thread-safety:
 * <ul>
 *   <li>Доступ ко всем общим состояниям (owner и counter) возможен только при удержании key.</li>
 * </ul>
 *
 * <p>Reentrancy:
 * <ul>
 *   <li>Если текущий поток уже владеет блокировкой,
 *   функция lock() просто увеличивает количество удержаний(counter).</li>
 *   <li>Блокировка снимается для других потоков только тогда, когда значение counter достигает нуля.</li>
 * </ul>
 */
public class MyReentrantLock {
    static String MESSAGE_ERROR_FACTORY_NULL = "factory не может быть null!";
    static String MESSAGE_ERROR_UNLOCK_OTHER = "Другой поток удерживает блокировку!";

    private int counter = 0;
    private Thread owner = null;
    private final NonReentrantLock key;
    private final NonReentrantLockFactory factory;

    /** Backoff параметры. */
    private static final int MAX_BACKOFF = 1_024;
    private static final int INITIAL_BACKOFF = 16;

    public MyReentrantLock(NonReentrantLockFactory factory) {
        if (factory == null) { throw new IllegalArgumentException(MESSAGE_ERROR_FACTORY_NULL); }
        this.factory = factory;
        this.key = factory.create();
    }

    /**
     * Захватывает reentrant lock.
     *
     * <p>Если текущий поток уже удерживает блокировку,
     * то данный метод просто увеличивает counter.</p>
     */
    public void lock() throws InterruptedException {
        Thread current = Thread.currentThread();
        int backoff = INITIAL_BACKOFF;

        for (;;) {
            key.lock();
            try {
                if (owner == null || owner == current) {
                    owner = current;
                    counter++;
                    return;
                }
            } finally { key.unlock(); }

            Thread.sleep(backoff);
            backoff = Math.min(backoff << 1, MAX_BACKOFF);
        }
    }

    /**
     * Уменьшает counter. При достижении counter нуля снимается блокировка для других потоков.
     *
     * <p>Если текущий поток не удерживает блокировку,
     * возникает исключение IllegalMonitorStateException.</p>
     */
    public void unlock() {
        Thread current = Thread.currentThread();

        key.lock();
        try {
            if (owner != current) { throw new IllegalMonitorStateException(MESSAGE_ERROR_UNLOCK_OTHER); }

            counter--;
            if (counter == 0) {owner = null; }
        } finally { key.unlock(); }
    }
}
