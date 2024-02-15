/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.security;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SecureVolleyRequestQueue {

    public static final int DEFAULT_TIMEOUT_IN_MS = 15000; // 15 sec.

    private final static String OIN = "";
    private final static String CERTIFICATE =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEkjCCA3qgAwIBAgIQCgFBQgAAAVOFc2oLheynCDANBgkqhkiG9w0BAQsFADA/\n" +
            "MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n" +
            "DkRTVCBSb290IENBIFgzMB4XDTE2MDMxNzE2NDA0NloXDTIxMDMxNzE2NDA0Nlow\n" +
            "SjELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUxldCdzIEVuY3J5cHQxIzAhBgNVBAMT\n" +
            "GkxldCdzIEVuY3J5cHQgQXV0aG9yaXR5IFgzMIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
            "AQ8AMIIBCgKCAQEAnNMM8FrlLke3cl03g7NoYzDq1zUmGSXhvb418XCSL7e4S0EF\n" +
            "q6meNQhY7LEqxGiHC6PjdeTm86dicbp5gWAf15Gan/PQeGdxyGkOlZHP/uaZ6WA8\n" +
            "SMx+yk13EiSdRxta67nsHjcAHJyse6cF6s5K671B5TaYucv9bTyWaN8jKkKQDIZ0\n" +
            "Z8h/pZq4UmEUEz9l6YKHy9v6Dlb2honzhT+Xhq+w3Brvaw2VFn3EK6BlspkENnWA\n" +
            "a6xK8xuQSXgvopZPKiAlKQTGdMDQMc2PMTiVFrqoM7hD8bEfwzB/onkxEz0tNvjj\n" +
            "/PIzark5McWvxI0NHWQWM6r6hCm21AvA2H3DkwIDAQABo4IBfTCCAXkwEgYDVR0T\n" +
            "AQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAYYwfwYIKwYBBQUHAQEEczBxMDIG\n" +
            "CCsGAQUFBzABhiZodHRwOi8vaXNyZy50cnVzdGlkLm9jc3AuaWRlbnRydXN0LmNv\n" +
            "bTA7BggrBgEFBQcwAoYvaHR0cDovL2FwcHMuaWRlbnRydXN0LmNvbS9yb290cy9k\n" +
            "c3Ryb290Y2F4My5wN2MwHwYDVR0jBBgwFoAUxKexpHsscfrb4UuQdf/EFWCFiRAw\n" +
            "VAYDVR0gBE0wSzAIBgZngQwBAgEwPwYLKwYBBAGC3xMBAQEwMDAuBggrBgEFBQcC\n" +
            "ARYiaHR0cDovL2Nwcy5yb290LXgxLmxldHNlbmNyeXB0Lm9yZzA8BgNVHR8ENTAz\n" +
            "MDGgL6AthitodHRwOi8vY3JsLmlkZW50cnVzdC5jb20vRFNUUk9PVENBWDNDUkwu\n" +
            "Y3JsMB0GA1UdDgQWBBSoSmpjBH3duubRObemRWXv86jsoTANBgkqhkiG9w0BAQsF\n" +
            "AAOCAQEA3TPXEfNjWDjdGBX7CVW+dla5cEilaUcne8IkCJLxWh9KEik3JHRRHGJo\n" +
            "uM2VcGfl96S8TihRzZvoroed6ti6WqEBmtzw3Wodatg+VyOeph4EYpr/1wXKtx8/\n" +
            "wApIvJSwtmVi4MFU5aMqrSDE6ea73Mj2tcMyo5jMd6jmeWUHK8so/joWUoHOUgwu\n" +
            "X4Po1QYz+3dszkDqMp4fklxBwXRsW10KXzPMTZ+sOPAveyxindmjkW8lGy+QsRlG\n" +
            "PfZ+G6Z6h7mjem0Y+iWlkYcV4PIWL1iwBi8saCbGS5jN2p8M+X+Q7UNKEkROb3N6\n" +
            "KOqkqm57TH2H3eDJAkSnh6/DNFu0Qg==\n" +
            "-----END CERTIFICATE-----\n";

    private static SecureVolleyRequestQueue instance = null;
    private static Context mCtx;
    private RequestQueue mRequestQueue;


    private SecureVolleyRequestQueue(final Context context) {
        this.mCtx = context;
    }

    public static synchronized SecureVolleyRequestQueue getInstance(final Context context) {
        if (instance == null) {
            instance = new SecureVolleyRequestQueue(context);
            instance.createRequestQueue();
        }
        return instance;
    }

    public RequestQueue getSecureRequestQueue() {
        return this.mRequestQueue;
    }

    private void createRequestQueue() {
        // gebruik deze alleen voor debug doeleinden
        this.mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext(), oauthHurlStack());
    }

    private HurlStack oauthHurlStack() {
        return new HurlStack() {

            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) super.createConnection(url);

                try {
                    // override the hostname verifier and de sslfactory
                    httpsURLConnection.setSSLSocketFactory(newSslSocketFactory());
                    httpsURLConnection.setHostnameVerifier(myHostnameVerifier());
                } catch (Exception e) {
                    //#IFDEF 'ontw'
                    //e.printStackTrace();
                    //#ENDIF
                }
                return httpsURLConnection;
            }
        };
    }

    /**
     * optioneel. Kan gebruikt worden om hostname verificatie te omzeilen bij bv. self-signed certificaten.
     *
     * @return hostnameverifier
     */
    private HostnameVerifier myHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                if (OIN == null) {
                    //return true;
                }

                // host tegen certificaat common name check
                // serial = gelijk OIN dienst

                HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();

                ///* controleer of de hostname van de sessie overeenkomt met de common name
                //in het certificaat */
                X509Certificate cert;
                try {
                    cert = (X509Certificate) session.getPeerCertificates()[0];
                } catch (SSLPeerUnverifiedException e) {
                    /* the identity of the peer is not verified. */
                    return false;
                }

                List<String> oids = Arrays.asList(cert.getSubjectDN().getName().split(","));
                //#IFDEF 'debug'
                Log.d("SEC", "oids: " + oids);
                //#ENDIF

                String cn = "";
                String certOIN = "";
                Iterator it = oids.iterator();
                while (it.hasNext()) {
                    String s = (String) it.next();
                    if (s.startsWith("CN=")) {
                        cn = s.substring(3);
                    }
                    if (s.startsWith("2.5.4.5=")) {
                        certOIN = s.substring(13);

                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < certOIN.length(); i += 2) {
                            String str = certOIN.substring(i, i + 2);
                            sb.append((char) Integer.parseInt(str, 16));
                        }
                        certOIN = sb.toString();
                    }
                }

                /* 1. servernaam moet gelijk zijn aan de CN in het certificaat
                   2. OIN nummer moet gelijk zijn aan wat er hardcoded opgeslagen is
                */
                //#IFDEF 'debug'
                Log.d("SEC", "cn equals servername: " + cn + "=" + hostname);
                Log.d("SEC", "oin equals stored oin: " + certOIN + "=" + OIN);
                //#ENDIF
                return (cn.equalsIgnoreCase(hostname) && certOIN.equalsIgnoreCase(OIN));
            }
        };
    }

    private SSLSocketFactory newSslSocketFactory() {
        try {
            //// Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");
            //// create a new keystore
            trusted.load(null, "ABC123".toCharArray());

            InputStream stream = new ByteArrayInputStream(CERTIFICATE.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(stream);

            trusted.setCertificateEntry("trustedca", cert);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(trusted);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
