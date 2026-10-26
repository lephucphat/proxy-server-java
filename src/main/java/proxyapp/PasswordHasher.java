package proxyapp;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordHasher {

    private static final int ROUNDS = 10000;
    private static final int MAX_ROUNDS = 1000000;

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2$" + ROUNDS + "$" + toHex(salt) + "$" + toHex(derive(password, salt, ROUNDS));
    }

    public static boolean matches(String password, String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals("pbkdf2")) {
            return false;
        }
        int rounds;
        byte[] salt;
        byte[] expected;
        try {
            rounds = Integer.parseInt(parts[1]);
            salt = fromHex(parts[2]);
            expected = fromHex(parts[3]);
        } catch (RuntimeException e) {
            return false;
        }
        if (rounds < 1 || rounds > MAX_ROUNDS) {
            return false;
        }
        byte[] actual = derive(password == null ? "" : password, salt, rounds);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(String password, byte[] salt, int rounds) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không băm được mật khẩu: " + e.getMessage());
        }
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String s) {
        if (s.length() % 2 != 0) {
            throw new IllegalArgumentException("Chuỗi hex sai");
        }
        byte[] data = new byte[s.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return data;
    }
}
