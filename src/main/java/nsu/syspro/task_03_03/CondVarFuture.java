package nsu.syspro.task_03_03;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Блокирующий Future, использующий Lock и Condition для синхронизации,
 * и представляющий собой отложенный результат задачи.
 *
 * @param <V> тип значения, возвращаемого задачей
 */
class CondVarFuture<V> {

    private final Lock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();

    private V result = null;
    private boolean done = false;
    private ExecutionException err = null;

    /**
     * Ждет, завершения вычисления, а затем возвращает результат.
     *
     * @return вычисленный результат
     * @throws ExecutionException если вычисление вызвало исключение
     */
    public V get() throws ExecutionException {
        try {
            lock.lock();

            while (!done) { cond.await(); }
            if (err != null) { throw err; }
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally { lock.unlock(); }
    }

    /**
     * Возвращает true, если задача завершена или брошено исключение.
     *
     * @return true, если задача завершена или брошено исключение, иначе false.
     */
    public boolean isDone() {
        try { lock.lock(); return done; }
        finally { lock.unlock(); }
    }

    /** Сохраняет результат, выставляет флаг, что задача выполнена. */
    void setResult(V result) {
        try {
            lock.lock();
            this.done = true;
            this.result = result;
            cond.signalAll();
        } finally { lock.unlock(); }
    }

    /** Сохраняет исключение, выставляет флаг, что задача выполнена. */
    void setException(Throwable t) {
        try {
            lock.lock();
            this.done = true;
            this.err = new ExecutionException(t);
            cond.signalAll();
        } finally { lock.unlock(); }
    }
}
