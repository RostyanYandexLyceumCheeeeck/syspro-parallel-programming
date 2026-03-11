package nsu.syspro.task_01_01;


class C extends Thread {
    public void run() {
        Thread b = Program2.b; b.start();
        try { while (!Program2.end_B); b.join(); System.out.println("C finished normal"); }
        catch (InterruptedException e) { /* заглушка */ }
    }
}

public class Program2 extends Program1 {
    public static void main(String[] args) {
        Thread c = new C();
        runProgram();
        c.start();
    }
}
