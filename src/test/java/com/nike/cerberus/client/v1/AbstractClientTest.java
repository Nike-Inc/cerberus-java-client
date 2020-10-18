package com.nike.cerberus.client.v1;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.nike.cerberus.client.auth.CerberusCredentials;

import okhttp3.OkHttpClient;

public abstract class AbstractClientTest {

	public OkHttpClient buildHttpClient(int timeout, TimeUnit timeoutUnit) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeout, timeoutUnit)
                .writeTimeout(timeout, timeoutUnit)
                .readTimeout(timeout, timeoutUnit)
                .build();
    }

    public String getResponseJson(final String title) {
        InputStream inputStream = getClass().getResourceAsStream(
                String.format("/com/nike/cerberus/client/%s.json", title));
        try {
            return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
    
    public String getResponseJson(final String object,final String title) {
        InputStream inputStream = getClass().getResourceAsStream(
                String.format("/com/nike/cerberus/client/%s/%s.json", object,title));
        try {
            return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public class TestCerberusCredentials implements CerberusCredentials {
        @Override
        public String getToken() {
            return "TOKEN";
        }
    }
}
