package proxyapp;

import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleInstanceTest {

    private SingleInstance first;

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @After
    public void tearDown() {
        if (first != null) {
            first.close();
        }
    }

    @Test
    public void secondCopyIsRefusedAndTheFirstIsToldToShowItsWindow() throws Exception {
        int port = freePort();
        first = new SingleInstance(port);
        assertTrue(first.tryLock());
        final CountDownLatch shown = new CountDownLatch(1);
        first.setOnShow(new Runnable() {
            public void run() {
                shown.countDown();
            }
        });
        SingleInstance second = new SingleInstance(port);
        assertFalse(second.tryLock());
        assertTrue(shown.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void requestThatArrivesBeforeTheHandlerIsStillDelivered() throws Exception {
        int port = freePort();
        first = new SingleInstance(port);
        assertTrue(first.tryLock());
        assertFalse(new SingleInstance(port).tryLock());
        Thread.sleep(300);
        final CountDownLatch shown = new CountDownLatch(1);
        first.setOnShow(new Runnable() {
            public void run() {
                shown.countDown();
            }
        });
        assertTrue(shown.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void lockCanBeTakenAgainAfterTheFirstCopyCloses() throws Exception {
        int port = freePort();
        first = new SingleInstance(port);
        assertTrue(first.tryLock());
        first.close();
        SingleInstance again = new SingleInstance(port);
        try {
            assertTrue(again.tryLock());
        } finally {
            again.close();
        }
    }

    @Test
    public void repeatedRequestsCallTheHandlerEachTime() throws Exception {
        int port = freePort();
        first = new SingleInstance(port);
        assertTrue(first.tryLock());
        final CountDownLatch twice = new CountDownLatch(2);
        first.setOnShow(new Runnable() {
            public void run() {
                twice.countDown();
            }
        });
        new SingleInstance(port).tryLock();
        new SingleInstance(port).tryLock();
        assertTrue(twice.await(5, TimeUnit.SECONDS));
        assertEquals(0, twice.getCount());
    }
}
