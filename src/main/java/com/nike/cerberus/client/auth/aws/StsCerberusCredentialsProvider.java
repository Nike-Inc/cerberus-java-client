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

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.regions.Regions;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nike.cerberus.client.CerberusClientException;
import com.nike.cerberus.client.auth.TokenCerberusCredentials;
import com.nike.cerberus.client.http.HttpMethod;
import com.nike.cerberus.client.http.HttpStatus;
import com.nike.cerberus.client.model.CerberusAuthResponse;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for allowing users to authenticate with Cerberus with the STS auth endpoint.
 */

public class StsCerberusCredentialsProvider extends BaseAwsCredentialsProvider {

    protected static String regionName;

    protected static final int DEFAULT_AUTH_RETRIES = 3;

    protected static final int DEFAULT_RETRY_INTERVAL_IN_MILLIS = 200;

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAwsCredentialsProvider.class);

    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    /**
     * Constructor to setup credentials provider
     *
     * @param cerberusUrl Cerberus URL
     * @param region AWS Region used in auth with Cerberus
     */
    public StsCerberusCredentialsProvider(String cerberusUrl, String region) {
        super(cerberusUrl);

        if (region != null ) {
            regionName = Regions.fromName(region).getName();
        } else {
            throw new CerberusClientException("Region is null. Please provide valid AWS region.");
        }
    }

    /**
     * Constructor to setup credentials provider
     *
     * @param cerberusUrl Cerberus URL
     * @param region AWS Region used in auth with Cerberus
     * @param xCerberusClientOverride Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public StsCerberusCredentialsProvider(String cerberusUrl, String region, String xCerberusClientOverride) {
        super(cerberusUrl, xCerberusClientOverride);
        if (region != null ) {
            regionName = Regions.fromName(region).getName();
        } else {
            throw new CerberusClientException("Region is null. Please provide valid AWS region.");
        }
    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link OkHttpClient}
     *
     * @param cerberusUrl Cerberus URL
     * @param region AWS Region used in auth with Cerberus
     * @param httpClient the client for interacting with Cerberus
     */
    public StsCerberusCredentialsProvider(String cerberusUrl, String region, OkHttpClient httpClient) {
        super(cerberusUrl, httpClient);
        if (region != null ) {
            regionName = Regions.fromName(region).getName();
        } else {
            throw new CerberusClientException("Region is null. Please provide valid AWS region.");
        }
    }

    /**
     * Obtains AWS Credentials.
     */
    private AWSCredentials getAWSCredentials(){
        return DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
    }

    /**
     * Signs request using AWS V4 signing.
     * @param request AWS STS request to sign
     * @param credentials AWS credentials
     */
    private void signRequest(com.amazonaws.Request request, AWSCredentials credentials){

        AWS4Signer signer = new AWS4Signer();
        signer.setRegionName(regionName);
        signer.setServiceName("sts");
        signer.sign(request, credentials);
    }

    /**
     * Generates and returns signed headers.
     */
    protected Map<String, String> getSignedHeaders(){

        final String url = "https://sts." + regionName + ".amazonaws.com";

        URI endpoint = null;

        try {
            endpoint = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("Action", Arrays.asList("GetCallerIdentity"));
        parameters.put("Version", Arrays.asList("2011-06-15"));

        DefaultRequest<String> requestToSign = new DefaultRequest<>("sts");
        requestToSign.setParameters(parameters);
        requestToSign.setHttpMethod(HttpMethodName.POST);
        requestToSign.setEndpoint(endpoint);

        LOGGER.info(String.format("Signing request with [%s] as host", url));

        signRequest(requestToSign, getAWSCredentials());

        return requestToSign.getHeaders();
    }

    /**
     * Sends request with signed headers to Cerberus to obtain token using STS Auth.
     */
    protected CerberusAuthResponse getToken(){

        if (StringUtils.isBlank(cerberusUrl)) {
            throw new CerberusClientException("Unable to find the Cerberus URL.");
        }

        LOGGER.info(String.format("Attempting to authenticate against [%s]", cerberusUrl));

        Map<String, String> signedHeaders = getSignedHeaders();

        try {
            Request request = new Request.Builder()
                    .url(cerberusUrl + "/v2/auth/sts-identity")
                    .headers(Headers.of(signedHeaders))
                    .method(HttpMethod.POST, RequestBody.create(DEFAULT_MEDIA_TYPE, ""))
                    .build();

            Response response = executeRequestWithRetry(request, DEFAULT_AUTH_RETRIES, DEFAULT_RETRY_INTERVAL_IN_MILLIS);

            if (response.code() != HttpStatus.OK) {
                parseAndThrowErrorResponse(response.code(), response.body().string());
            }

            return gson.fromJson(response.body().string(), CerberusAuthResponse.class);

        } catch (IOException e) {
            throw new CerberusClientException("I/O error while communicating with Cerberus", e);
        }
    }

    /**
     * Requests a token from Cerberus using STS Auth and sets the token and expiration details.
     */
    @Override
    protected void authenticate() {

        CerberusAuthResponse token = getToken();

        if (token.getClientToken() != null) {
            LOGGER.info(String.format("Authentication successful"));
        } else {
            throw new CerberusClientException("Success response from Cerberus missing token");
        }

        final DateTime expires = DateTime.now(DateTimeZone.UTC)
                .plusSeconds(token.getLeaseDuration() - paddingTimeInSeconds);
        credentials = new TokenCerberusCredentials(token.getClientToken());
        expireDateTime = expires;
    }
}
