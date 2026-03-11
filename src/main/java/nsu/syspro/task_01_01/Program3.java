package nsu.syspro.task_01_01;


class D extends Thread {
    public void run() {
        Thread a = Program3.a; a.start();
        try { a.join(); System.out.println("D: A finished normal"); }
        catch (InterruptedException e) { /* заглушка */ }
    }
}


public class Program3 extends Program1 {
    public static void main(String[] args) {
        Thread d = new D();
        runProgram();
        d.start();
    }
}
