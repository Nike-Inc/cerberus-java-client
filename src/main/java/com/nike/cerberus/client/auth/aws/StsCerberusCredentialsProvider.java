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
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nike.cerberus.client.CerberusClientException;
import com.nike.cerberus.client.ClientVersion;
import com.nike.cerberus.client.UrlResolver;
import com.nike.cerberus.client.http.HttpMethod;
import com.nike.cerberus.client.http.HttpStatus;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for allowing users to authenticate with Cerberus with the STS auth endpoint
 */

public class StsCerberusCredentialsProvider extends BaseAwsCredentialsProvider {

    private static final String REGION_STRING = "us-east-1";
    protected static final int DEFAULT_AUTH_RETRIES = 3;

    protected static final int DEFAULT_RETRY_INTERVAL_IN_MILLIS = 200;
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     */
    public StsCerberusCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);

    }


    public GetCallerIdentityResult getCallerIdentity() {
        final AWSSecurityTokenService sts = AWSSecurityTokenServiceClientBuilder.defaultClient();

        GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest();

        GetCallerIdentityResult callerIdentity = sts.getCallerIdentity(getCallerIdentityRequest);
        return callerIdentity;
    }

    public AWSCredentials getAWSCredentials(){
        return DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
    }

    private void signRequest(com.amazonaws.Request request, AWSCredentials credentials){
        AWS4Signer signer = new AWS4Signer();
        signer.setRegionName(REGION_STRING);
        signer.setServiceName("sts");
        signer.sign(request, credentials);

    }

    public Map<String, String> getSignedHeaders(){

//        final String url = this.getUrlResolver().resolve();
//        final String url = "https://sts.amazonaws.com";
        final String url = "https://sts.us-east-1.amazonaws.com";

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

        signRequest(requestToSign, getAWSCredentials());

        return requestToSign.getHeaders();
    }

    public String buildRequest(final String iamPrincipalArn, Region region){


        Map<String, String> signedHeaders = getSignedHeaders();

        final String url = this.getUrlResolver().resolve();

//        Map<String, List<String>> parameters = new HashMap<>();
//        parameters.put("Action", Arrays.asList("GetCallerIdentity"));
//        parameters.put("Version", Arrays.asList("2011-06-15"));

//        if (StringUtils.isBlank(url)) {
//            throw new CerberusClientException("Unable to find the Cerberus URL.");
//        }

//        LOGGER.info(String.format("Attempting to authenticate with AWS IAM principal ARN [%s] against [%s]",
//                iamPrincipalArn, url));

        try {

            Request request = new Request.Builder()
                    .url(url + "/v2/auth/sts-identity")
                    .headers(Headers.of(signedHeaders))
                    .method(HttpMethod.POST, buildCredentialsRequestBody(iamPrincipalArn, region))
//                    .post(body)
                    .build();
//
////            okhttp3.Request.Builder requestBuilder = new Request.Builder().url(url + "/v2/auth/sts-identity");
////                    .addHeader(HttpHeader.ACCEPT, DEFAULT_MEDIA_TYPE.toString())
////                    .addHeader(HttpHeader.CONTENT_TYPE, DEFAULT_MEDIA_TYPE.toString())
////                    .addHeader(ClientVersion.CERBERUS_CLIENT_HEADER, cerberusJavaClientHeaderValue)
////                    .method(HttpMethod.POST, buildCredentialsRequestBody(iamPrincipalArn, region));
////                    .method(HttpMethod.POST);
//            Request builtRequest = new Request(requestBuilder);

//            Response response = executeRequestWithRetry(requestBuilder.build(), DEFAULT_AUTH_RETRIES, DEFAULT_RETRY_INTERVAL_IN_MILLIS);
            Response response = executeRequestWithRetry(request, DEFAULT_AUTH_RETRIES, DEFAULT_RETRY_INTERVAL_IN_MILLIS);

            if (response.code() != HttpStatus.OK) {
                parseAndThrowErrorResponse(response.code(), response.body().string());
            }

            final Type mapType = new TypeToken<Map<String, String>>() {
            }.getType();
            final Map<String, String> authData = gson.fromJson(response.body().string(), mapType);
            final String key = "auth_data";

            if (authData.containsKey(key)) {
//                LOGGER.info(String.format("Authentication successful with AWS IAM principal ARN [%s] against [%s]",
//                        iamPrincipalArn, url));
                return authData.get(key);
            } else {
                throw new CerberusClientException("Success response from IAM role authenticate endpoint missing auth data!");
            }

        } catch (IOException e) {
            throw new CerberusClientException("I/O error while communicating with Cerberus", e);
        }

    }

    @Override
    protected void authenticate() {

    }

}
