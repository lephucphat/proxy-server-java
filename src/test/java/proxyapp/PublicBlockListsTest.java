package proxyapp;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublicBlockListsTest {

    private File dir;
    private File cache;
    private BlockList blockList;
    private PublicBlockLists lists;
    private HttpServer server;

    @Before
    public void setUp() throws Exception {
        dir = Files.createTempDirectory("publist").toFile();
        cache = new File(dir, "public.txt");
        blockList = new BlockList(new File(dir, "user.txt"));
        lists = new PublicBlockLists(blockList, cache);
    }

    @After
    public void tearDown() {
        lists.stopAuto();
        if (server != null) {
            server.stop(0);
        }
        for (File f : dir.listFiles()) {
            f.delete();
        }
        dir.delete();
    }

    private String write(String name, String text) throws Exception {
        File f = new File(dir, name);
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return f.getAbsolutePath();
    }

    private String serve(final int code, final String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(code, data.length);
            OutputStream os = ex.getResponseBody();
            os.write(data);
            os.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/list.txt";
    }

    @Test
    public void mergesSeveralFileSources() throws Exception {
        String a = write("a.txt", "0.0.0.0 ads.one.com\n0.0.0.0 track.two.net\n");
        String b = write("b.txt", "evil.org\nads.one.com\n");
        PublicBlockLists.Result r = lists.update(Arrays.asList(a, b));
        assertTrue(r.anyOk);
        assertEquals(3, r.domains);
        assertEquals(3, lists.size());
        assertTrue(blockList.matches("ads.one.com"));
        assertTrue(blockList.matches("www.evil.org"));
        assertFalse(blockList.matches("good.example"));
        assertEquals(2, r.messages.size());
    }

    @Test
    public void publicListIsSeparateFromHandTypedList() throws Exception {
        blockList.setText("mine.example\n");
        lists.update(Arrays.asList(write("a.txt", "pub.example\n")));
        assertEquals(1, blockList.size());
        assertEquals(1, blockList.publicSize());
        assertTrue(blockList.matches("mine.example"));
        assertTrue(blockList.matches("pub.example"));
        blockList.setText("");
        assertTrue(blockList.matches("pub.example"));
        assertFalse(blockList.matches("mine.example"));
    }

    @Test
    public void reportsBadSourceButKeepsGoodOne() throws Exception {
        String good = write("good.txt", "ok.example\n");
        PublicBlockLists.Result r = lists.update(Arrays.asList(new File(dir, "khong-co.txt").getPath(), good));
        assertTrue(r.anyOk);
        assertTrue(blockList.matches("ok.example"));
        assertTrue(r.messages.get(0).contains("lỗi"));
    }

    @Test
    public void failedUpdateKeepsOldList() throws Exception {
        lists.update(Arrays.asList(write("a.txt", "old.example\n")));
        PublicBlockLists.Result r = lists.update(Arrays.asList(new File(dir, "khong-co.txt").getPath()));
        assertFalse(r.anyOk);
        assertTrue(blockList.matches("old.example"));
        assertEquals(1, r.domains);
    }

    @Test
    public void cacheSurvivesRestart() throws Exception {
        lists.update(Arrays.asList(write("a.txt", "saved.example\nother.example\n")));
        assertTrue(cache.isFile());
        BlockList fresh = new BlockList(new File(dir, "user2.txt"));
        PublicBlockLists again = new PublicBlockLists(fresh, cache);
        again.loadCache();
        assertTrue(fresh.matches("saved.example"));
        assertEquals(2, again.size());
        assertTrue(again.getUpdatedAt() > 0);
    }

    @Test
    public void loadCacheWithoutFileDoesNothing() {
        lists.loadCache();
        assertEquals(0, lists.size());
        assertEquals(0, lists.getUpdatedAt());
    }

    @Test
    public void downloadsFromHttpSource() throws Exception {
        String url = serve(200, "0.0.0.0 http.example\n");
        PublicBlockLists.Result r = lists.update(Arrays.asList(url));
        assertTrue(r.anyOk);
        assertTrue(blockList.matches("http.example"));
    }

    @Test
    public void httpErrorIsReported() throws Exception {
        String url = serve(404, "nope");
        PublicBlockLists.Result r = lists.update(Arrays.asList(url));
        assertFalse(r.anyOk);
        assertTrue(r.messages.get(0).contains("404"));
        assertEquals(0, lists.size());
    }

    @Test
    public void tooBigSourceIsRejected() throws Exception {
        lists.maxBytes = 100;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("d").append(i).append(".example\n");
        }
        PublicBlockLists.Result r1 = lists.update(Arrays.asList(write("big.txt", sb.toString())));
        assertFalse(r1.anyOk);
        PublicBlockLists.Result r2 = lists.update(Arrays.asList(serve(200, sb.toString())));
        assertFalse(r2.anyOk);
        assertEquals(0, lists.size());
    }

    @Test
    public void splitsSourcesByLineAndSkipsComments() {
        List<String> s = PublicBlockLists.splitSources(" http://a/x \r\n\n# bo qua\nD:\\thu muc\\b.txt\n");
        assertEquals(Arrays.asList("http://a/x", "D:\\thu muc\\b.txt"), s);
        assertTrue(PublicBlockLists.splitSources(null).isEmpty());
        assertTrue(PublicBlockLists.splitSources("  \n").isEmpty());
    }

    @Test
    public void autoUpdateRunsInBackground() throws Exception {
        List<String> sources = new ArrayList<String>();
        sources.add(write("auto.txt", "auto.example\n"));
        lists.startAuto(sources, 50, 3600000);
        long end = System.currentTimeMillis() + 5000;
        while (!blockList.matches("auto.example") && System.currentTimeMillis() < end) {
            Thread.sleep(50);
        }
        assertTrue(blockList.matches("auto.example"));
    }

    @Test
    public void scheduleIsNotStartedWithoutSourcesOrHours() throws Exception {
        String a = write("a.txt", "never.example\n");
        lists.applySchedule(new ArrayList<String>(), 24);
        lists.applySchedule(Arrays.asList(a), 0);
        Thread.sleep(300);
        assertFalse(blockList.matches("never.example"));
    }
}
