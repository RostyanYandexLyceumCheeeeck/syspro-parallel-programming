package nsu.syspro.task_03_03;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;


public class SingleThreadExecutorServiceTest {
    private SingleThreadExecutorService service;

    @BeforeEach
    public void setUp() {
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) { return new Thread(r); }
        };
        service = new SingleThreadExecutorService(factory);
    }

    /**
     * Тест 1: Простое выполнение задачи с возвратом значения.
     */
    @Test
    public void testBasicSubmission() throws ExecutionException {
        Callable<String> task = () -> "Hello from task!";
        CondVarFuture<String> future = service.submit(task);

        String result = future.get();
        assertEquals("Hello from task!", result);
        assertTrue(future.isDone());
    }

    /**
     * Тест 2: Выполнение нескольких задач последовательно.
     */
    @Test
    public void testSequentialExecution() throws ExecutionException {
        List<Integer> orderLog = new ArrayList<>();
        Callable<Void> task1 = () -> { orderLog.add(1); return null; };
        Callable<Void> task2 = () -> { orderLog.add(2); return null; };
        Callable<Void> task3 = () -> { orderLog.add(3); return null; };

        CondVarFuture<Void> f1 = service.submit(task1);
        CondVarFuture<Void> f2 = service.submit(task2);
        CondVarFuture<Void> f3 = service.submit(task3);

        f1.get(); f2.get(); f3.get();

        assertEquals(List.of(1, 2, 3), orderLog);
    }

    /**
     * Тест 3: Вызов задачи, которая бросает checked исключение.
     */
    @Test
    public void testCheckedExceptionHandling() {
        Callable<String> task = () -> {
            throw new Exception("This is a checked exception");
        };
        CondVarFuture<String> future = service.submit(task);

        ExecutionException thrown = assertThrows(ExecutionException.class, future::get);
        assertTrue(thrown.getCause().getMessage().contains("This is a checked exception"));
        assertTrue(future.isDone());
    }

    /**
     *: Вызов задачи, которая бросает unchecked исключение (RuntimeException).
     * Рабочий поток должен завершиться, но сервис должен создать новый.
     */
    @Test
    public void testUncheckedExceptionRestart() throws ExecutionException, InterruptedException {
        AtomicLong thread1Id = new AtomicLong(-1);
        AtomicLong thread2Id = new AtomicLong(-1);

        Callable<String> taskThatFails = () -> {
            thread1Id.set(Thread.currentThread().getId());
            throw new RuntimeException("This thread will die now");
        };
        CondVarFuture<String> future1 = service.submit(taskThatFails);

        ExecutionException thrown = assertThrows(ExecutionException.class, future1::get);
        assertTrue(thrown.getCause().getMessage().contains("This thread will die now"));

        // Подождем немного, чтобы старый поток точно умер и начался новый
        Thread.sleep(200);

        // Отправим новую задачу. Она должна выполниться в новом потоке.
        Callable<String> taskThatSucceeds = () -> {
            thread2Id.set(Thread.currentThread().getId());
            return "Success after restart";
        };
        CondVarFuture<String> future2 = service.submit(taskThatSucceeds);

        String result = future2.get();
        assertEquals("Success after restart", result);
        assertTrue(future2.isDone());
        
        assertNotEquals(-1L, thread1Id.get());
        assertNotEquals(-1L, thread2Id.get());
        assertNotEquals(thread1Id.get(), thread2Id.get());
    }

    /**
     * Тест 5: Вызов задачи, которая бросает Error (например, OutOfMemoryError).
     * Это должно привести к завершению потока и перезапуску, как в тесте 4.
     */
    @Test
    public void testErrorRestart() throws ExecutionException, InterruptedException {
        AtomicLong thread1Id = new AtomicLong(-1);
        AtomicLong thread2Id = new AtomicLong(-1);

        Callable<String> taskThatErrors = () -> {
            thread1Id.set(Thread.currentThread().getId());
            throw new OutOfMemoryError("");
        };
        CondVarFuture<String> future1 = service.submit(taskThatErrors);

        ExecutionException thrown = assertThrows(ExecutionException.class, future1::get);
        assertInstanceOf(OutOfMemoryError.class, thrown.getCause());

        Thread.sleep(200);

        Callable<String> taskThatSucceeds = () -> {
            thread2Id.set(Thread.currentThread().getId());
            return "Success after error";
        };
        CondVarFuture<String> future2 = service.submit(taskThatSucceeds);

        String result = future2.get();
        assertEquals("Success after error", result);
        
        assertNotEquals(-1L, thread1Id.get());
        assertNotEquals(-1L, thread2Id.get());
        assertNotEquals(thread1Id.get(), thread2Id.get());
    }


    /**
     * Проверяет, что задачи в сервисе выполняются строго последовательно в рамках одного потока.
     *
     * Тест имитирует длительное выполнение первой задачи и проверяет, что вторая задача
     * не начинает выполняться параллельно (находится в очереди), а после завершения первой
     * выполняется тем же самым потоком.
     */
    @Test
    public void testSingleThreadExecutionWithConditions() throws Exception {
        Lock lock = new ReentrantLock();
        Condition startedTask1 = lock.newCondition();
        Condition finishedTask1 = lock.newCondition();

        AtomicLong thread1Id = new AtomicLong(-1);
        AtomicLong thread2Id = new AtomicLong(-1);

        // защита от spurios wakeup
        boolean[] flags = new boolean[2];

        Callable<Void> task1 = () -> {
            lock.lock();
            try {
                flags[0] = true;
                thread1Id.set(Thread.currentThread().getId());
                startedTask1.signal();

                while (!flags[1]) { finishedTask1.await(); }
            } finally { lock.unlock(); }
            return null;
        };

        Callable<Void> task2 = () -> {
            thread2Id.set(Thread.currentThread().getId());
            return null;
        };

        CondVarFuture<Void> future1 = service.submit(task1);
        CondVarFuture<Void> future2 = service.submit(task2);

        lock.lock();
        try { while (!flags[0]) { startedTask1.await(); } } 
        finally { lock.unlock(); }

        assertFalse(future2.isDone());

        lock.lock();
        try { flags[1] = true; finishedTask1.signal(); } 
        finally { lock.unlock(); }

        future1.get();
        future2.get();

        assertNotEquals(-1L, thread1Id.get());
        assertEquals(thread1Id.get(), thread2Id.get());
    }
}