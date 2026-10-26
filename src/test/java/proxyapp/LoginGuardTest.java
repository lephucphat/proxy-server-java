package proxyapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoginGuardTest {

    private long time = 1000;

    private LoginGuard make(int max, long lockMillis) {
        return new LoginGuard(max, lockMillis) {
            long now() {
                return time;
            }
        };
    }

    @Test
    public void locksAfterTooManyFailures() {
        LoginGuard g = make(3, 60000);
        g.failure("1.1.1.1");
        g.failure("1.1.1.1");
        assertFalse(g.isLocked("1.1.1.1"));
        g.failure("1.1.1.1");
        assertTrue(g.isLocked("1.1.1.1"));
    }

    @Test
    public void failureTellsWhenTheLockStartsOnlyOnce() {
        LoginGuard g = make(3, 60000);
        assertFalse(g.failure("1.1.1.1"));
        assertFalse(g.failure("1.1.1.1"));
        assertTrue(g.failure("1.1.1.1"));
        assertFalse(make(0, 60000).failure("1.1.1.1"));
    }

    @Test
    public void otherAddressesAreNotAffected() {
        LoginGuard g = make(2, 60000);
        g.failure("1.1.1.1");
        g.failure("1.1.1.1");
        assertTrue(g.isLocked("1.1.1.1"));
        assertFalse(g.isLocked("2.2.2.2"));
    }

    @Test
    public void lockEndsAfterLockTime() {
        LoginGuard g = make(1, 5000);
        g.failure("1.1.1.1");
        assertTrue(g.isLocked("1.1.1.1"));
        time += 4999;
        assertTrue(g.isLocked("1.1.1.1"));
        time += 2;
        assertFalse(g.isLocked("1.1.1.1"));
        g.failure("1.1.1.1");
        assertTrue(g.isLocked("1.1.1.1"));
    }

    @Test
    public void successClearsFailureCount() {
        LoginGuard g = make(3, 60000);
        g.failure("1.1.1.1");
        g.failure("1.1.1.1");
        g.success("1.1.1.1");
        g.failure("1.1.1.1");
        g.failure("1.1.1.1");
        assertFalse(g.isLocked("1.1.1.1"));
    }

    @Test
    public void oldFailuresAreForgotten() {
        LoginGuard g = make(3, 5000);
        g.failure("1.1.1.1");
        g.failure("1.1.1.1");
        time += 6000;
        g.failure("1.1.1.1");
        assertFalse(g.isLocked("1.1.1.1"));
    }

    @Test
    public void zeroMeansNeverLock() {
        LoginGuard g = make(0, 60000);
        for (int i = 0; i < 20; i++) {
            g.failure("1.1.1.1");
        }
        assertFalse(g.isLocked("1.1.1.1"));
    }
}
