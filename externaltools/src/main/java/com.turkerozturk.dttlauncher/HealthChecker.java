/*
 * This file is part of the DailyTopicTracker project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/springtopiccalendar
 *
 * Copyright (c) 2025 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.dttlauncher;

import javax.net.ssl.*;
import java.net.*;
import java.io.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class HealthChecker {

    static {
        // HTTPS için self-signed sertifika doğrulamasını bypass et
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Hostname verification bypass
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isApplicationUp(String healthUrl) {
        try {
            URL url = new URL(healthUrl);
            URLConnection urlConnection = url.openConnection();

            if (urlConnection instanceof HttpsURLConnection httpsConn) {
                httpsConn.setConnectTimeout(1000);
                httpsConn.setReadTimeout(2000);
                return checkHealthResponse(httpsConn);
            } else if (urlConnection instanceof HttpURLConnection httpConn) {
                httpConn.setConnectTimeout(1000);
                httpConn.setReadTimeout(2000);
                return checkHealthResponse(httpConn);
            }
        } catch (IOException e) {
            // Sessiz geç: UP değil veya bağlantı hatası
        }
        return false;
    }

    private boolean checkHealthResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        if (code == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb.indexOf("\"status\":\"UP\"") >= 0;
            }
        }
        return false;
    }
}
