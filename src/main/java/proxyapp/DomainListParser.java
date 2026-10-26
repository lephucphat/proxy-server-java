package proxyapp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DomainListParser {

    private static final Set<String> IGNORED = new HashSet<String>(Arrays.asList(
            "localhost.localdomain", "local.localdomain", "broadcasthost", "ip6-localhost", "ip6-loopback",
            "ip6-localnet", "ip6-mcastprefix", "ip6-allnodes", "ip6-allrouters", "ip6-allhosts"));

    public static Set<String> parse(String text) {
        Set<String> result = new HashSet<String>();
        for (String line : text.split("\\r?\\n")) {
            parseLine(line, result);
        }
        return result;
    }

    private static void parseLine(String line, Set<String> result) {
        String s = line.trim();
        if (s.isEmpty() || s.startsWith("#") || s.startsWith("!") || s.startsWith("[")) {
            return;
        }
        int hash = s.indexOf('#');
        if (hash > 0) {
            s = s.substring(0, hash).trim();
        }
        if (s.startsWith("||")) {
            s = s.substring(2);
            int end = firstOf(s, "^/$:");
            if (end >= 0) {
                s = s.substring(0, end);
            }
            add(s, result);
            return;
        }
        String[] parts = s.split("\\s+");
        if (parts.length == 1) {
            add(parts[0], result);
        } else if (isIp(parts[0])) {
            for (int i = 1; i < parts.length; i++) {
                add(parts[i], result);
            }
        }
    }

    private static int firstOf(String s, String chars) {
        for (int i = 0; i < s.length(); i++) {
            if (chars.indexOf(s.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isIp(String s) {
        return s.matches("\\d{1,3}(\\.\\d{1,3}){3}") || (s.indexOf(':') >= 0 && s.matches("[0-9a-fA-F:.]+"));
    }

    private static void add(String candidate, Set<String> result) {
        String domain = BlockList.normalize(candidate);
        if (domain == null || IGNORED.contains(domain)) {
            return;
        }
        if (!domain.matches("[a-z0-9_-]+(\\.[a-z0-9_-]+)+")) {
            return;
        }
        String lastLabel = domain.substring(domain.lastIndexOf('.') + 1);
        if (lastLabel.matches("\\d+")) {
            return;
        }
        result.add(domain);
    }
}
