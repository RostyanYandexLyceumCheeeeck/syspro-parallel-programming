package nsu.syspro.task_01_06;

public class DeadlockDuo {
    static volatile Runnable lambda = null;
    public static void main(String... args) throws Exception {
        Thread A = new Thread(() -> { lambda.run(); });
        Thread B = new Thread(() -> { try { A.join(); } catch (InterruptedException e) {/**/} } );
        lambda = () -> { try { B.join(); } catch (InterruptedException e) {/**/} };
        A.start(); B.start();
        A.join(); B.join();
    }
}
