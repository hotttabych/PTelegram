package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FakePasscodeSerializer {
    public static byte[] serializeEncrypted(FakePasscode passcode, String passcodeString) {
        try {
            byte[] fakePasscodeBytes = getJsonMapper().writeValueAsString(passcode).getBytes("UTF-8");

            byte[] initializationVector = new byte[16];
            Utilities.random.nextBytes(initializationVector);
            byte[] key = MessageDigest.getInstance("MD5").digest(passcodeString.getBytes("UTF-8"));
            byte[] encryptedBytes = encryptBytes(compress(fakePasscodeBytes), initializationVector, key, false);
            byte[] resultBytes = new byte[16 + encryptedBytes.length];
            System.arraycopy(initializationVector, 0, resultBytes, 0, 16);
            System.arraycopy(encryptedBytes, 0, resultBytes, 16, encryptedBytes.length);
            return resultBytes;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static FakePasscode deserializeEncrypted(byte[] encryptedPasscodeData, String passcodeString) {
        try {
            byte[] initializationVector = Arrays.copyOfRange(encryptedPasscodeData, 0, 16);
            byte[] key = MessageDigest.getInstance("MD5").digest(passcodeString.getBytes("UTF-8"));
            byte[] encryptedPasscode = Arrays.copyOfRange(encryptedPasscodeData, 16, encryptedPasscodeData.length);
            byte[] decryptedBytes = encryptBytes(encryptedPasscode, initializationVector, key, true);
            FakePasscode passcode = getJsonMapper().readValue(new String(decompress(decryptedBytes)), FakePasscode.class);
            passcode.passcodeHash = calculateHash(passcodeString, SharedConfig.passcodeSalt);
            return passcode;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] encryptBytes(byte[] data, byte[] initializationVector, byte[] key, boolean isDecrypt) throws Exception {
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(isDecrypt ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
        return cipher.doFinal(data);
    }

    public static String calculateHash(String password, byte[] salt) {
        try {
            byte[] passcodeBytes = password.getBytes("UTF-8");
            byte[] bytes = new byte[32 + passcodeBytes.length];
            System.arraycopy(salt, 0, bytes, 0, 16);
            System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
            System.arraycopy(salt, 0, bytes, passcodeBytes.length + 16, 16);
            return Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static byte[] compress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DeflaterOutputStream defl = new DeflaterOutputStream(out);
            defl.write(in);
            defl.flush();
            defl.close();

            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] decompress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream infl = new InflaterOutputStream(out);
            infl.write(in);
            infl.flush();
            infl.close();

            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static ObjectMapper jsonMapper = null;
    private static ObjectMapper getJsonMapper() {
        if (jsonMapper != null) {
            return jsonMapper;
        }
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.registerModule(new KotlinModule());
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        jsonMapper.setVisibility(jsonMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return jsonMapper;
    }
}
