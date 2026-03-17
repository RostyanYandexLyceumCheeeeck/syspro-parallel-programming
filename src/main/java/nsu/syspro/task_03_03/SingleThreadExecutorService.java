package nsu.syspro.task_03_03;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Сервис исполнителя, который использует один рабочий поток, работающий с неограниченной очередью.
 * Если рабочий поток завершает работу из-за сбоя при выполнении, вместо него запускается новый.
 */
class SingleThreadExecutorService {

    private final ThreadFactory factory;
    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    /** Внутренний класс, реализующий логику выполнения задач. */
    private class TaskRunner implements Runnable {
        @Override
        public void run() {
            try { while (!Thread.currentThread().isInterrupted()) { queue.take().run(); } }
            catch (Throwable t) { Thread.currentThread().interrupt(); }
            finally { startNewWorker(); }
        }
    }

    /** Внутренний класс, управляющий Future. */
    private static class RunnableFuture<V> implements Runnable {
        private final Callable<V> task;
        private final CondVarFuture<V> future;

        public RunnableFuture(Callable<V> task, CondVarFuture<V> future) {
            this.task = task;
            this.future = future;
        }

        @Override
        public void run() {
            try { future.setResult(task.call()); }
            catch (ExecutionException e) { future.setException(e); }
            catch (Throwable t) { future.setException(t); throw new RuntimeException(t); }
        }
    }

    /**
     * Создает сервис с указанной фабрикой потоков.
     *
     * @param f фабрика, используемая для создания нового потока.
     */
    public SingleThreadExecutorService(ThreadFactory f) {
        this.factory = f;
        startNewWorker();
    }

    /**
     * Перезапускает рабочий поток. Этот метод вызывается, когда текущий поток завершает работу.
     */
    private void startNewWorker() {
//        System.out.println("startNewWorker!");
        factory.newThread(new TaskRunner()).start();
    }

    /**
     * Добавляет задачу в очередь и возвращает объект CondVarFuture.
     *
     * @param task задача для выполнения
     * @param <T> тип возвращаемого значения задачи
     * @return CondVarFuture
     */
    public <T> CondVarFuture<T> submit(Callable<T> task) {
        CondVarFuture<T> future = new CondVarFuture<>();
        RunnableFuture<T> futureTask = new RunnableFuture<>(task, future);

        queue.offer(futureTask);
        return future;
    }
}
