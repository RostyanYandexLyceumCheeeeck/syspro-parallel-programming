package nsu.syspro.task_01_01;


class B extends Thread {
    public void run() {
        System.out.println("B start");
        try { throw new RuntimeException("Error in B!"); }
        finally { Program1.end_B = true; }
    }
}


class A extends Thread {
    public void run() {
        Thread b = Program1.b; b.start();
        try { b.join(); System.out.println("A finished normal"); }
        catch (InterruptedException e) { /* заглушка */ }
    }
}


public class Program1 {
    public static Thread a, b;
    public static volatile boolean end_B = false;

    public static void main(String[] args) {
        runProgram();
        a.start();
    }

    public static void runProgram() { a = new A(); b = new B(); }
}
