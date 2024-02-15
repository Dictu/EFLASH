/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.security;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

public class PrivatePreferences {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String PRIVATE_PREFS_KEY = "PrivatePreferencesKey";

    private Context context;
    private KeyStore keyStore;

    public PrivatePreferences(Context context) {
        this.context = context;

        this.createAlias(this.getKeyStore(), this.getAlias());
    }

    public String getString(String key, String defaultValue) {
        final String value = this.getPrivatePreferences().getString(key, null);
        if (value == null) {
            return defaultValue;
        }

        final String decryptedValue = this.decryptValue(this.getKeyStore(), this.getAlias(), value);
        if (decryptedValue == null) {
            return defaultValue;
        }
        return decryptedValue;
    }

    public void putString(String key, String value) {
        final SharedPreferences.Editor editor = this.getPrivatePreferences().edit();

        final String encryptedValue = this.encryptValue(this.getKeyStore(), this.getAlias(), value);
        if (encryptedValue != null) {
            editor.putString(key, encryptedValue);
            editor.commit();
        }
    }

    public void remove(String key) {
        final SharedPreferences.Editor editor = this.getPrivatePreferences().edit();
        editor.remove(key);
        editor.commit();
    }

    private KeyStore getKeyStore() {
        if (this.keyStore != null) {
            return this.keyStore;
        }

        try {
            this.keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            this.keyStore.load(null);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
        }
        return this.keyStore;
    }

    private String getAlias() {
        return this.context.getPackageName();
    }

    private void createAlias(KeyStore keyStore, String alias) {
        try {
            if (!keyStore.containsAlias(alias)) {
                final Calendar start = Calendar.getInstance();
                final Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 30);

                final KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                    final KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
//                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
//                            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
//                            .setKeyValidityStart(start.getTime())
//                            .setKeyValidityEnd(end.getTime())
//                            .build();
//                    generator.initialize(spec);
//                } else {
                final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(this.context)
                        .setAlias(alias)
                        .setSubject(new X500Principal("CN=Dienst ICT Uitvoering, O=Ministerie van Economische Zaken"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                generator.initialize(spec);
//                }

                generator.generateKeyPair();
            }
        } catch (IllegalStateException e) {
            Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));

            this.context.startActivity(new Intent("com.android.credentials.UNLOCK"));
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
        }
    }

    private String encryptValue(KeyStore keyStore, String alias, String value) {
        try {
            final KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
            final RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            final Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            input.init(Cipher.ENCRYPT_MODE, publicKey);

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, input);
            cipherOutputStream.write(value.getBytes("UTF-8"));
            cipherOutputStream.close();

            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
            return null;
        }
    }

    private String decryptValue(KeyStore keyStore, String alias, String value) {
        try {
            final KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);

            final Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());

            final CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(Base64.decode(value, Base64.DEFAULT)), output);

            final ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte) nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

            return new String(bytes, 0, bytes.length, "UTF-8");
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
            return null;
        }
    }

    private SharedPreferences getPrivatePreferences() {
        return this.context.getSharedPreferences(PRIVATE_PREFS_KEY, Context.MODE_PRIVATE);
    }

}
