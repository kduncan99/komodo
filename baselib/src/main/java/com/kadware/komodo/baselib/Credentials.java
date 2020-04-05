/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;

public class Credentials {

    public final String _userName;
    public final String _salt;
    public final String _hashedPassword;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int HASH_LENGTH = 32;
    private static final int ITERATIONS = 17;
    private static final int SALT_BYTES = 64;

    @JsonCreator
    public Credentials(
        @JsonProperty("userName") final String userName,
        @JsonProperty("salt") final String salt,
        @JsonProperty("hashedPassword") final String hashedPassword
    ) {
        System.out.println(String.format("Loading from JSON: userName=%s", userName));//TODO remove
        System.out.println(String.format("                 : salt=%s", salt));//TODO this too
        System.out.println(String.format("                 : hashedPassword=%s", hashedPassword));//TODO and this

        _userName = userName;
        if ((salt == null) || (hashedPassword == null)) {
            _salt = getSecureSalt();
            _hashedPassword = computeHash("admin", _salt);
        } else {
            _salt = salt;
            _hashedPassword = hashedPassword;
        }

        System.out.println(String.format("Final Result     : userName=%s", _userName));//TODO remove
        System.out.println(String.format("                 : salt=%s", _salt));//TODO this too
        System.out.println(String.format("                 : hashedPassword=%s", _hashedPassword));//TODO and this
    }

    public Credentials(
        final String userName,
        final String clearTextPassword
    ) {
        System.out.println(String.format("Loading from JSON: userName=%s", userName));//TODO remove
        System.out.println(String.format("                 : password=%s", clearTextPassword));//TODO this too

        _userName = userName;
        _salt = getSecureSalt();
        _hashedPassword = computeHash(clearTextPassword, _salt);

        System.out.println(String.format("Final Result     : userName=%s", _userName));//TODO remove
        System.out.println(String.format("                 : salt=%s", _salt));//TODO this too
        System.out.println(String.format("                 : hashedPassword=%s", _hashedPassword));//TODO and this
    }

    public static String computeHash(
        final String password,
        final String salt
    ) {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), ITERATIONS, HASH_LENGTH * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hashBytes = factory.generateSecret(keySpec).getEncoded();
            return hexByteToString(hashBytes);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            throw new RuntimeException("Caught " + ex.getMessage());
        }
    }

    public static String getSecureSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] saltBytes = new byte[SALT_BYTES];
        sr.nextBytes(saltBytes);
        return hexByteToString(saltBytes);
    }

    private static byte[] hexByteFromString(
        final String value
    ) {
        int vlen = value.length();
        if ((vlen == 0) || ((vlen % 2) != 0)) {
            throw new RuntimeException("Invalid length for conversion");
        }

        byte[] result = new byte[vlen / 2];
        for (int bx = 0, vx = 0; vx < vlen; bx++, vx += 2) {
            result[bx] = (byte)Integer.parseInt(value.substring(vx, vx + 2),16);
        }

        return result;
    }

    private static String hexByteToString(
        final byte[] value
    ) {
        StringBuilder sb = new StringBuilder();
        for (byte b : value) {
            sb.append(String.format("%02X", (int)b & 0xFF));
        }
        return sb.toString();
    }

    public boolean validatePassword(
        final String clearTextPassword
    ) {
        System.out.println(String.format("validate: ctPassword=%s", clearTextPassword));//TODO remove this
        String computedHash = computeHash(clearTextPassword, _salt);
        boolean result = computedHash.equals(_hashedPassword);
        System.out.println(String.format("  reuslt: %s", result));//TODO and this
        return result;
    }
}
