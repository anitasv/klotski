package me.anitas;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class DynCountDownLatch  {

    private final AtomicInteger internal = new AtomicInteger(1);

    private final Semaphore semaphore = new Semaphore(0);

    public void register() {
        internal.incrementAndGet();
    }

    public void countDown() {
        int c = internal.decrementAndGet();
        if (c == 0) {
            System.out.println("Release");
            semaphore.release();
        }
    }

    public void await() throws InterruptedException {
        countDown();
        semaphore.acquire();
        register();
    }
}
