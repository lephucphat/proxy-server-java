package proxyapp;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UserStoreTest {

    private File root;
    private File file;
    private File dir;
    private UserStore store;

    @Before
    public void setUp() throws Exception {
        root = Files.createTempDirectory("userstore").toFile();
        file = new File(root, "users.txt");
        dir = new File(root, "users");
        store = new UserStore(file, dir);
        store.load();
    }

    @After
    public void tearDown() {
        delete(root);
    }

    private static void delete(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                delete(c);
            }
        }
        f.delete();
    }

    private static void expectBad(Runnable r) {
        try {
            r.run();
            fail("phải báo lỗi");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void addedAccountCanBeFoundAndChecked() throws Exception {
        store.add("be.an", "matkhau1");
        UserStore.Account a = store.find("be.an");
        assertNotNull(a);
        assertTrue(a.checkPassword("matkhau1"));
        assertFalse(a.checkPassword("sai"));
        assertFalse(a.ownRules);
        assertNull(store.find("khong-co"));
        assertNull(store.find(null));
        assertEquals(1, store.size());
    }

    @Test
    public void accountsSurviveReloadAndPasswordsAreNotStoredAsPlainText() throws Exception {
        store.add("an", "matkhau-an");
        store.add("binh", "matkhau-binh");
        store.setRules("binh", true, true, "hoc.vn\n", "game.com * 00:00-24:00\n");
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertFalse(text.contains("matkhau"));
        UserStore again = new UserStore(file, dir);
        again.load();
        assertEquals(2, again.size());
        assertTrue(again.find("an").checkPassword("matkhau-an"));
        UserStore.Account b = again.find("binh");
        assertTrue(b.checkPassword("matkhau-binh"));
        assertTrue(b.ownRules);
        assertTrue(b.allowOnly);
        assertTrue(b.allowList.matches("www.hoc.vn"));
        assertEquals(1, b.schedule.size());
        assertEquals("an", again.list().get(0).name);
        assertEquals("binh", again.list().get(1).name);
    }

    @Test
    public void badNamesAreRejected() {
        String[] bad = {"", " ", "co dau cach", "..", ".an", "a/b", "a\\b", "a:b", "tên", "a\tb",
            "0123456789012345678901234567890123"};
        for (final String name : bad) {
            expectBad(new Runnable() {
                public void run() {
                    try {
                        store.add(name, "matkhau");
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        assertEquals(0, store.size());
    }

    @Test
    public void shortPasswordAndDuplicateNamesAreRejected() throws Exception {
        expectBad(new Runnable() {
            public void run() {
                try {
                    store.add("an", "abc");
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        store.add("An", "matkhau");
        expectBad(new Runnable() {
            public void run() {
                try {
                    store.add("an", "matkhau");
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        assertEquals(1, store.size());
    }

    @Test
    public void passwordCanBeChanged() throws Exception {
        store.add("an", "cu-cu-cu");
        store.setPassword("an", "moi-moi-moi");
        UserStore again = new UserStore(file, dir);
        again.load();
        assertTrue(again.find("an").checkPassword("moi-moi-moi"));
        assertFalse(again.find("an").checkPassword("cu-cu-cu"));
    }

    @Test
    public void removingDeletesAccountAndItsRuleFiles() throws Exception {
        store.add("an", "matkhau");
        assertTrue(new File(dir, "an.allow.txt").isFile());
        assertTrue(new File(dir, "an.schedule.txt").isFile());
        store.remove("an");
        assertNull(store.find("an"));
        assertFalse(new File(dir, "an.allow.txt").exists());
        assertFalse(new File(dir, "an.schedule.txt").exists());
        UserStore again = new UserStore(file, dir);
        again.load();
        assertEquals(0, again.size());
    }

    @Test
    public void badScheduleKeepsTheOldRules() throws Exception {
        store.add("an", "matkhau");
        store.setRules("an", true, false, "a.vn\n", "x.com * 00:00-24:00\n");
        try {
            store.setRules("an", false, true, "b.vn\n", "hong hong");
            fail();
        } catch (IllegalArgumentException expected) {
        }
        UserStore.Account a = store.find("an");
        assertTrue(a.ownRules);
        assertFalse(a.allowOnly);
        assertTrue(a.allowList.matches("a.vn"));
        assertFalse(a.allowList.matches("b.vn"));
        assertEquals(1, a.schedule.size());
    }

    @Test
    public void unknownAccountIsAnError() throws Exception {
        try {
            store.setPassword("ai-do", "matkhau");
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            store.remove("ai-do");
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void brokenLinesAndDuplicatesInTheFileAreSkipped() throws Exception {
        String good = PasswordHasher.hash("matkhau");
        String text = "an\t" + good + "\t0\t0\n"
                + "hong\n"
                + "../x\t" + good + "\t0\t0\n"
                + "AN\t" + good + "\t0\t0\n"
                + "rong\t\t0\t0\n"
                + "binh\t" + good + "\t1\t1\n";
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        UserStore again = new UserStore(file, dir);
        again.load();
        assertEquals(2, again.size());
        assertNotNull(again.find("an"));
        assertTrue(again.find("binh").ownRules);
    }

    @Test
    public void storeWithoutFilesWorksInMemory() throws Exception {
        UserStore memory = new UserStore(null, new File(root, "mem"));
        memory.add("an", "matkhau");
        assertTrue(memory.find("an").checkPassword("matkhau"));
    }
}
