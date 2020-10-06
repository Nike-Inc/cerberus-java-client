package com.nike.cerberus.client.factory;

import static okhttp3.ConnectionSpec.CLEARTEXT;
import static okhttp3.ConnectionSpec.MODERN_TLS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.TlsVersion;

public abstract class BaseClientFactory {

    public static final int DEFAULT_TIMEOUT = 30_000;
    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    
    /**
     * Modify "MODERN_TLS" to remove TLS v1.0 and 1.1
     */
    public static final ConnectionSpec TLS_1_2_OR_NEWER = new ConnectionSpec.Builder(MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build();

    /**
     * A CerberusV2Client may need to make many requests to Cerberus simultaneously.
     * <p>
     * (Default value in OkHttpClient for maxRequests was 64 and maxRequestsPerHost was 5).
     */
    protected static final int DEFAULT_MAX_REQUESTS = 200;
    protected static final Map<String, String> DEFAULT_HEADERS = new HashMap<>();
    
    protected static List<ConnectionSpec> getConnectionSpecs(){
        List<ConnectionSpec> connectionSpecs = new ArrayList<>();
        connectionSpecs.add(TLS_1_2_OR_NEWER);
        // for unit tests
        connectionSpecs.add(CLEARTEXT);
        return connectionSpecs;
    }
    
    protected static Headers.Builder getHeaders(Map<String, String> defaultHeaders){
        if (defaultHeaders == null) {
            throw new IllegalArgumentException("Default headers cannot be null.");
        }
    	
        Headers.Builder headers = new Headers.Builder();
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            headers.add(header.getKey(), header.getValue());
        }
        return headers;
    }
    
    protected static Dispatcher getDispatcher(int maxRequests,int maxRequestsPerHost) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
        return dispatcher;
    }


	
}
