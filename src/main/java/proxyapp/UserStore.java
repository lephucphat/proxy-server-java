package proxyapp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

public class UserStore {

    public static class Account {
        public final String name;
        volatile String passwordHash;
        public volatile boolean ownRules;
        public volatile boolean allowOnly;
        public final BlockList allowList;
        public final BlockSchedule schedule;

        Account(String name, String passwordHash, boolean ownRules, boolean allowOnly, File dir) {
            this.name = name;
            this.passwordHash = passwordHash;
            this.ownRules = ownRules;
            this.allowOnly = allowOnly;
            this.allowList = new BlockList(new File(dir, name + ".allow.txt"), BlockList.ALLOW_DEFAULT_TEXT);
            this.schedule = new BlockSchedule(new File(dir, name + ".schedule.txt"));
            this.allowList.load();
            this.schedule.load();
        }

        public boolean checkPassword(String plain) {
            return PasswordHasher.matches(plain, passwordHash);
        }
    }

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]{0,31}");
    private static final int MIN_PASSWORD = 4;

    private final File file;
    private final File dir;
    private final Map<String, Account> accounts = new ConcurrentSkipListMap<String, Account>();

    public UserStore(File file, File dir) {
        this.file = file;
        this.dir = dir;
    }

    public synchronized void load() {
        accounts.clear();
        if (file == null || !file.isFile()) {
            return;
        }
        String text;
        try {
            text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return;
        }
        for (String line : text.split("\\r?\\n")) {
            String[] f = line.split("\t", -1);
            if (f.length != 4 || !NAME.matcher(f[0]).matches() || f[1].isEmpty() || existsIgnoreCase(f[0])) {
                continue;
            }
            makeDir();
            accounts.put(f[0], new Account(f[0], f[1], f[2].equals("1"), f[3].equals("1"), dir));
        }
    }

    public Account find(String name) {
        return name == null ? null : accounts.get(name);
    }

    public List<Account> list() {
        return new ArrayList<Account>(accounts.values());
    }

    public int size() {
        return accounts.size();
    }

    public static void checkName(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Tên tài khoản chỉ gồm chữ không dấu, số, _ . - (tối đa 32 ký tự, không bắt đầu bằng dấu chấm)");
        }
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất " + MIN_PASSWORD + " ký tự");
        }
    }

    private boolean existsIgnoreCase(String name) {
        for (String n : accounts.keySet()) {
            if (n.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void makeDir() {
        if (dir != null) {
            dir.mkdirs();
        }
    }

    public synchronized Account add(String name, String password) throws IOException {
        checkName(name);
        checkPassword(password);
        if (existsIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên tài khoản đã có");
        }
        makeDir();
        Account a = new Account(name, PasswordHasher.hash(password), false, false, dir);
        accounts.put(name, a);
        save();
        return a;
    }

    public synchronized void setPassword(String name, String password) throws IOException {
        Account a = require(name);
        checkPassword(password);
        a.passwordHash = PasswordHasher.hash(password);
        save();
    }

    public synchronized void remove(String name) throws IOException {
        Account a = require(name);
        accounts.remove(name);
        new File(dir, a.name + ".allow.txt").delete();
        new File(dir, a.name + ".schedule.txt").delete();
        save();
    }

    public synchronized void setRules(String name, boolean ownRules, boolean allowOnly, String allowText, String scheduleText)
            throws IOException {
        Account a = require(name);
        a.schedule.setText(scheduleText);
        a.allowList.setText(allowText);
        a.ownRules = ownRules;
        a.allowOnly = allowOnly;
        save();
    }

    private Account require(String name) {
        Account a = accounts.get(name);
        if (a == null) {
            throw new IllegalArgumentException("Không có tài khoản " + name);
        }
        return a;
    }

    private void save() throws IOException {
        if (file == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Account a : accounts.values()) {
            sb.append(a.name).append('\t').append(a.passwordHash).append('\t')
                    .append(a.ownRules ? "1" : "0").append('\t').append(a.allowOnly ? "1" : "0").append('\n');
        }
        File temp = new File(file.getPath() + ".tmp");
        Files.write(temp.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
