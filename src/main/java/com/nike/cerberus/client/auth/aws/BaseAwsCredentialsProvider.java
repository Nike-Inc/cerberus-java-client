/*
 * Copyright (c) 2018 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.client.auth.aws;

import com.nike.cerberus.client.CerberusServerException;
import com.nike.cerberus.client.ClientVersion;
import com.nike.cerberus.client.auth.CerberusCredentials;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.auth.TokenCerberusCredentials;
import okhttp3.*;
import org.apache.commons.lang3.NotImplementedException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.nike.cerberus.client.CerberusClientFactory.*;
import static okhttp3.ConnectionSpec.CLEARTEXT;

/**
 * {@link CerberusCredentialsProvider} implementation that uses some AWS
 * credentials provider to authenticate with Cerberus and decrypt the auth
 * response using STS Auth. If the assigned role has been granted the appropriate
 * provisioned for usage of Cerberus, it will succeed and have a token that can be
 * used to interact with Cerberus.
 */
public class BaseAwsCredentialsProvider implements CerberusCredentialsProvider {

    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAwsCredentialsProvider.class);

    protected static final int DEFAULT_AUTH_RETRIES = 3;

    protected static final int DEFAULT_RETRY_INTERVAL_IN_MILLIS = 200;

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    protected final int paddingTimeInSeconds = 60;

    protected volatile TokenCerberusCredentials credentials;

    protected volatile DateTime expireDateTime = DateTime.now().minus(paddingTimeInSeconds);

    protected final String cerberusUrl;

    private final String cerberusJavaClientHeaderValue;

    private final OkHttpClient httpClient;

    /**
     * Constructor to setup credentials provider
     *
     * @param cerberusUrl Cerberus URL
     */
    public BaseAwsCredentialsProvider(String cerberusUrl) {
        super();
        this.cerberusUrl = cerberusUrl;
        this.cerberusJavaClientHeaderValue = ClientVersion.getClientHeaderValue();
        LOGGER.info("Cerberus URL={}", this.cerberusUrl);

        this.httpClient = createHttpClient();
    }

    /**
     * Constructor to setup credentials provider
     *
     * @param cerberusUrl             Cerberus URL
     * @param xCerberusClientOverride Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public BaseAwsCredentialsProvider(String cerberusUrl, String xCerberusClientOverride) {
        super();
        this.cerberusUrl = cerberusUrl;
        this.cerberusJavaClientHeaderValue = xCerberusClientOverride;
        LOGGER.info("Cerberus URL={}", this.cerberusUrl);

        this.httpClient = createHttpClient();
    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link OkHttpClient}
     *
     * @param cerberusUrl Cerberus URL
     * @param httpClient  the client to use for auth
     */
    public BaseAwsCredentialsProvider(String cerberusUrl, OkHttpClient httpClient) {
        super();
        this.cerberusUrl = cerberusUrl;
        this.cerberusJavaClientHeaderValue = ClientVersion.getClientHeaderValue();
        LOGGER.info("Cerberus URL={}", this.cerberusUrl);

        this.httpClient = httpClient;
    }

    /**
     * Returns the Cerberus credentials. If none have been acquired yet or has
     * expired, triggers a refresh.
     *
     * @return Cerberus credentials
     */
    @Override
    public CerberusCredentials getCredentials() {
        readLock.lock();
        try {
            boolean needsToAuthenticate = false;
            if (credentials == null) {
                // initial state: no credentials
                needsToAuthenticate = true;
            } else if (expireDateTime.isBeforeNow()) {
                // credentials have expired
                needsToAuthenticate = true;
                LOGGER.info("Cerberus credentials have expired {}, re-authenticating...", expireDateTime);
            }
            if (needsToAuthenticate) {
                // Release the read lock and acquire a write lock
                readLock.unlock();
                writeLock.lock();

                try {
                    authenticate();
                } finally {
                    // Acquire the read lock before releasing the write lock
                    readLock.lock();
                    writeLock.unlock();
                }
            }

            return new TokenCerberusCredentials(credentials.getToken());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Look up the IAM roles associated with the given AWS credentials provider
     * and attempt to authenticate and decrypt using STS Auth.
     */
     protected void authenticate()
     {
         throw new NotImplementedException("method authenticate must be overridden");
     }

    /**
     * Executes an HTTP request and retries if a 500 level error is returned
     *
     * @param request               The request to execute
     * @param numRetries            The maximum number of times to retry
     * @param sleepIntervalInMillis Time in milliseconds to sleep between retries. Zero for no sleep.
     * @return Any HTTP response with status code below 500, or the last error response if only 500's are returned
     * @throws IOException If an IOException occurs during the last retry, then rethrow the error
     */
    protected Response executeRequestWithRetry(Request request, int numRetries, int sleepIntervalInMillis) throws IOException {
        IOException exception = null;
        Response response = null;
        for (int retryNumber = 0; retryNumber < numRetries; retryNumber++) {
            try {
                response = httpClient.newCall(request).execute();
                if (response.code() < 500) {
                    return response;
                }
            } catch (IOException ioe) {
                LOGGER.debug(String.format("Failed to call %s %s. Retrying...", request.method(), request.url()), ioe);
                exception = ioe;
            }
            sleep(sleepIntervalInMillis * (long) Math.pow(2, retryNumber));
        }

        if (exception != null) {
            throw exception;
        } else {
            return response;
        }
    }

    private void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException ie) {
            LOGGER.warn("Sleep interval interrupted.", ie);
        }
    }

    protected void parseAndThrowErrorResponse(final int responseCode, final String responseBody) {
        final String message = String.format("Failed to authenticate. Response: %s", responseBody);
        LOGGER.warn(message);
        List<String> errors = new ArrayList<>(1);
        errors.add(message);
        throw new CerberusServerException(responseCode, errors);
    }

    public String getCerberusUrl(){
        return cerberusUrl;
    }

    public final OkHttpClient createHttpClient() {

        List<ConnectionSpec> connectionSpecs = new ArrayList<>();
        connectionSpecs.add(TLS_1_2_OR_NEWER);
        // for unit tests
        connectionSpecs.add(CLEARTEXT);

        return new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .connectionSpecs(connectionSpecs)
                .build();
    }
}
