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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.util.Base64;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nike.cerberus.client.CerberusClientException;
import com.nike.cerberus.client.CerberusServerException;
import com.nike.cerberus.client.ClientVersion;
import com.nike.cerberus.client.UrlResolver;
import com.nike.cerberus.client.auth.CerberusCredentials;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.auth.TokenCerberusCredentials;
import com.nike.cerberus.client.http.HttpHeader;
import com.nike.cerberus.client.http.HttpMethod;
import com.nike.cerberus.client.http.HttpStatus;
import com.nike.cerberus.client.model.CerberusAuthResponse;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.nike.cerberus.client.CerberusClientFactory.DEFAULT_TIMEOUT;
import static com.nike.cerberus.client.CerberusClientFactory.DEFAULT_TIMEOUT_UNIT;
import static com.nike.cerberus.client.CerberusClientFactory.TLS_1_2_OR_NEWER;
import static com.nike.cerberus.client.auth.aws.StaticIamRoleCerberusCredentialsProvider.IAM_ROLE_ARN_FORMAT;
import static okhttp3.ConnectionSpec.CLEARTEXT;

/**
 * {@link CerberusCredentialsProvider} implementation that uses some AWS
 * credentials provider to authenticate with Cerberus and decrypt the auth
 * response using KMS. If the assigned role has been granted the appropriate
 * provisioned for usage of Cerberus, it will succeed and have a token that can be
 * used to interact with Cerberus.
 */
public abstract class BaseAwsCredentialsProvider implements CerberusCredentialsProvider {

    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAwsCredentialsProvider.class);

    protected static final int DEFAULT_AUTH_RETRIES = 3;

    protected static final int DEFAULT_RETRY_INTERVAL_IN_MILLIS = 200;

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    protected final int paddingTimeInSeconds = 60;

    protected volatile TokenCerberusCredentials credentials;

    protected volatile DateTime expireDateTime = DateTime.now().minus(paddingTimeInSeconds);

    private final UrlResolver urlResolver;

    private final String cerberusJavaClientHeaderValue;

    private final OkHttpClient httpClient;

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     */
    public BaseAwsCredentialsProvider(UrlResolver urlResolver) {
        super();
        this.urlResolver = urlResolver;
        this.cerberusJavaClientHeaderValue = ClientVersion.getClientHeaderValue();
        LOGGER.info("Cerberus URL={}", urlResolver.resolve());

        this.httpClient = createHttpClient();

    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver             Resolver for resolving the Cerberus URL
     * @param xCerberusClientOverride Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public BaseAwsCredentialsProvider(UrlResolver urlResolver, String xCerberusClientOverride) {
        super();
        this.urlResolver = urlResolver;
        this.cerberusJavaClientHeaderValue = xCerberusClientOverride;
        LOGGER.info("Cerberus URL={}", urlResolver.resolve());

        this.httpClient = createHttpClient();
    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver} and {@link OkHttpClient}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     * @param httpClient  the client to use for auth
     */
    public BaseAwsCredentialsProvider(UrlResolver urlResolver, OkHttpClient httpClient) {
        super();
        this.urlResolver = urlResolver;
        this.cerberusJavaClientHeaderValue = ClientVersion.getClientHeaderValue();
        LOGGER.info("Cerberus URL={}", urlResolver.resolve());

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
     * and attempt to authenticate and decrypt using KMS.
     */
    abstract protected void authenticate();

    /**
     * Authenticates with Cerberus and decrypts and sets the token and expiration details.
     *
     * @param accountId   AWS account ID used to auth with cerberus
     * @param iamRoleName IAM role name used to auth with cerberus
     * @deprecated no longer used, will be removed
     */
    protected void getAndSetToken(final String accountId, final String iamRoleName) {
        final String iamRoleArn = String.format(IAM_ROLE_ARN_FORMAT, accountId, iamRoleName);

        getAndSetToken(iamRoleArn, Regions.getCurrentRegion());
    }

    /**
     * Authenticates with Cerberus and decrypts and sets the token and expiration details.
     *
     * @param iamPrincipalArn AWS IAM principal ARN used to auth with cerberus
     * @param region          AWS Region used in auth with cerberus
     */
    protected void getAndSetToken(final String iamPrincipalArn, final Region region) {
        final AWSKMSClient kmsClient = new AWSKMSClient();
        kmsClient.setRegion(region);

        final String encryptedAuthData = getEncryptedAuthData(iamPrincipalArn, region);
        final CerberusAuthResponse decryptedToken = decryptToken(kmsClient, encryptedAuthData);
        final DateTime expires = DateTime.now(DateTimeZone.UTC)
                .plusSeconds(decryptedToken.getLeaseDuration() - paddingTimeInSeconds);

        credentials = new TokenCerberusCredentials(decryptedToken.getClientToken());
        expireDateTime = expires;
    }

    /**
     * Retrieves the encrypted auth response from Cerberus.
     *
     * @param iamPrincipalArn IAM principal ARN used in the row key
     * @param region          Current region of the running function or instance
     * @return Base64 and encrypted token
     */
    protected String getEncryptedAuthData(final String iamPrincipalArn, Region region) {
        final String url = urlResolver.resolve();

        if (StringUtils.isBlank(url)) {
            throw new CerberusClientException("Unable to find the Cerberus URL.");
        }

        LOGGER.info(String.format("Attempting to authenticate with AWS IAM principal ARN [%s] against [%s]",
                iamPrincipalArn, url));

        try {
            Request.Builder requestBuilder = new Request.Builder().url(url + "/v2/auth/iam-principal")
                    .addHeader(HttpHeader.ACCEPT, DEFAULT_MEDIA_TYPE.toString())
                    .addHeader(HttpHeader.CONTENT_TYPE, DEFAULT_MEDIA_TYPE.toString())
                    .addHeader(ClientVersion.CERBERUS_CLIENT_HEADER, cerberusJavaClientHeaderValue)
                    .method(HttpMethod.POST, buildCredentialsRequestBody(iamPrincipalArn, region));

            Response response = executeRequestWithRetry(requestBuilder.build(), DEFAULT_AUTH_RETRIES, DEFAULT_RETRY_INTERVAL_IN_MILLIS);

            if (response.code() != HttpStatus.OK) {
                parseAndThrowErrorResponse(response.code(), response.body().string());
            }

            final Type mapType = new TypeToken<Map<String, String>>() {
            }.getType();
            final Map<String, String> authData = gson.fromJson(response.body().string(), mapType);
            final String key = "auth_data";

            if (authData.containsKey(key)) {
                LOGGER.info(String.format("Authentication successful with AWS IAM principal ARN [%s] against [%s]",
                        iamPrincipalArn, url));
                return authData.get(key);
            } else {
                throw new CerberusClientException("Success response from IAM role authenticate endpoint missing auth data!");
            }
        } catch (IOException e) {
            throw new CerberusClientException("I/O error while communicating with Cerberus", e);
        }
    }

    /**
     * Decodes the encrypted token and attempts to decrypt it using AWS KMS. If
     * successful, the token is returned.
     *
     * @param kmsClient      KMS client
     * @param encryptedToken Token to decode and decrypt
     * @return Decrypted token
     */
    protected CerberusAuthResponse decryptToken(AWSKMS kmsClient, String encryptedToken) {
        byte[] decodedToken;

        try {
            decodedToken = Base64.decode(encryptedToken);
        } catch (IllegalArgumentException iae) {
            throw new CerberusClientException("Encrypted token not Base64 encoded", iae);
        }

        final DecryptRequest request = new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(decodedToken));
        final DecryptResult result = kmsClient.decrypt(request);

        final String decryptedAuthData = new String(result.getPlaintext().array(), Charset.forName("UTF-8"));

        return gson.fromJson(decryptedAuthData, CerberusAuthResponse.class);
    }

    /**
     * Executes a request and retries n times if an undesired status is returned
     * @param request                The request to execute
     * @param numRetries             The number of times to retry
     * @param sleepIntervalInMillis  Time in milliseconds to pause between retries
     * @return The successful response returned upon executing the request
     * @throws IOException  If an IOException occurs during the last retry, then rethrow the error
     */
    protected Response executeRequestWithRetry(Request request, int numRetries, int sleepIntervalInMillis) throws IOException {
        IOException exception = null;
        Response response = null;
        for(int retryNumber = 0; retryNumber < numRetries; retryNumber++) {
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

    private RequestBody buildCredentialsRequestBody(final String iamPrincipalArn, Region region) {
        final String regionName = region == null ? Regions.getCurrentRegion().getName() : region.getName();

        final Map<String, String> credentials = new HashMap<>();
        credentials.put("iam_principal_arn", iamPrincipalArn);
        credentials.put("region", regionName);

        return RequestBody.create(DEFAULT_MEDIA_TYPE, gson.toJson(credentials));
    }

    private void parseAndThrowErrorResponse(final int responseCode, final String responseBody) {
        final String message = String.format("Failed to authenticate. Response: %s", responseBody);
        LOGGER.warn(message);
        List<String> errors = new ArrayList<>(1);
        errors.add(message);
        throw new CerberusServerException(responseCode, errors);
    }

    private OkHttpClient createHttpClient() {

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
