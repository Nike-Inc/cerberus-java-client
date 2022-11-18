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

package com.nike.cerberus.client;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.http.HttpHeader;
import com.nike.cerberus.client.http.HttpMethod;
import com.nike.cerberus.client.http.HttpStatus;
import com.nike.cerberus.client.model.CerberusCategoryResponse;
import com.nike.cerberus.client.model.CerberusListFilesResponse;
import com.nike.cerberus.client.model.CerberusListResponse;
import com.nike.cerberus.client.model.CerberusResponse;
import static io.github.resilience4j.decorators.Decorators.ofSupplier;

import com.nike.cerberus.client.model.CerberusRolePermission;
import com.nike.cerberus.client.model.CerberusRoleResponse;
import com.nike.cerberus.client.model.CerberusSafeDepositBoxRequest;
import com.nike.cerberus.client.model.CerberusSafeDepositBoxResponse;
import com.nike.cerberus.client.model.CerberusSafeDepositBoxSummaryResponse;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for interacting with a Cerberus.
 */
public class CerberusClient {

    public static final String SECRET_PATH_PREFIX = "v1/secret/";

    public static final String ROLE_PATH = "v1/role";

    public static final String CATEGORY_PATH = "v1/category";

    public static final String SAFE_DEPOSIT_BOX_PREFIX = "v2/safe-deposit-box/";

    public static final String SECURE_FILE_PATH_PREFIX = "v1/secure-file/";

    public static final String EMPTY_PATH = "";

    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    protected static final int DEFAULT_NUM_RETRIES = 3;

    private static final RetryConfig RETRY_CONFIG =
            RetryConfig.<Response>custom()
                    .maxAttempts(DEFAULT_NUM_RETRIES)
                    .retryOnResult( response -> response.code() >= 500 && response.code() <= 599)
                    .intervalFunction(
                            IntervalFunction.ofExponentialBackoff(Duration.of(250, ChronoUnit.MILLIS)))
                    .build();

    private final Retry RETRY = Retry.of(this.getClass().getName(), RETRY_CONFIG);

    private final CerberusCredentialsProvider credentialsProvider;

    private final OkHttpClient httpClient;

    private final String url;

    private final Headers defaultHeaders;

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .registerTypeAdapter(DateTime.class,
                    (JsonDeserializer<DateTime>) (json, typeOfT, context) -> new DateTime(json.getAsString()))
            .create();

    private static final Logger LOGGER = LoggerFactory.getLogger(CerberusClient.class);

    public CerberusClient(final String cerberusUrl,
                          final CerberusCredentialsProvider credentialsProvider,
                          final OkHttpClient httpClient,
                          final Headers defaultHeaders) {

        if (cerberusUrl == null) {
            throw new IllegalArgumentException("Cerberus URL cannot be null.");
        }

        if (credentialsProvider == null) {
            throw new IllegalArgumentException("Credentials provider cannot be null.");
        }

        if (httpClient == null) {
            throw new IllegalArgumentException("Http client cannot be null.");
        }

        if (defaultHeaders == null) {
            throw new IllegalArgumentException("Default headers cannot be null.");
        }

        this.url = cerberusUrl;
        this.credentialsProvider = credentialsProvider;
        this.httpClient = httpClient;
        this.defaultHeaders = defaultHeaders;
    }

    /**
     * Explicit constructor that allows for full control over construction of the Cerberus client.
     *
     * @param cerberusUrl          URL for Cerberus
     * @param credentialsProvider  Credential provider for acquiring a token for interacting with Cerberus
     * @param httpClient           HTTP client for calling Cerberus
     */
    public CerberusClient(final String cerberusUrl,
                          final CerberusCredentialsProvider credentialsProvider,
                          final OkHttpClient httpClient) {

        if (cerberusUrl == null) {
            throw new IllegalArgumentException("Cerberus URL resolver can not be null.");
        }

        if (credentialsProvider == null) {
            throw new IllegalArgumentException("Credentials provider can not be null.");
        }

        if (httpClient == null) {
            throw new IllegalArgumentException("Http client can not be null.");
        }

        this.url = cerberusUrl;
        this.credentialsProvider = credentialsProvider;
        this.httpClient = httpClient;
        this.defaultHeaders = new Headers.Builder().build();
    }

    /**
     * List operation for the specified path.  Will return a {@link Map} with a single entry of keys which is an
     * array of strings that represents the keys at that path. If Cerberus returns an unexpected response code, a
     * {@link CerberusServerException} will be thrown with the code and error details.  If an unexpected I/O error is
     * encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     * <p>
     * See https://www.cerberusproject.io/docs/secrets/generic/index.html for details on what the list operation returns.
     * </p>
     *
     * @param path Path to the data
     * @return Map containing the keys at that path
     */
    public CerberusListResponse list(final String path) {

        final HttpUrl httpUrl = buildUrl(SECRET_PATH_PREFIX, path + "?list=true");
        LOGGER.debug("list: requestUrl={}", httpUrl);

        final Response response = ofSupplier(
                    () -> execute(httpUrl, HttpMethod.GET, null)
            )
            .withRetry(RETRY)
            .decorate()
            .get();

        if (response.code() == HttpStatus.NOT_FOUND) {
            response.close();
            return new CerberusListResponse();
        } else if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        final Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        final Map<String, Object> rootData = parseResponseBody(response, mapType);
        return gson.fromJson(gson.toJson(rootData.get("data")), CerberusListResponse.class);
    }

    /**
     * Lists all files at the specified path. Will return a {@link Map} that contains a paginated list
     * of secure file summaries. If Cerberus returns an unexpected response code, a {@link CerberusServerException}
     * will be thrown with the code and error details.  If an unexpected I/O error is
     * encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     * <p>
     * See https://github.com/Nike-Inc/cerberus/blob/master/API.md for details on what the
     * list files operation returns.
     * </p>
     *
     * @param path Path to the data
     * @return Cerberus response object that lists file metadata
     */
    public CerberusListFilesResponse listFiles(final String path) {
        return listFiles(path, null, null);
    }

    /**
     * Lists all files at the specified path.  Will return a {@link Map} that contains a paginated list
     * of secure file summaries. If Cerberus returns an unexpected response code, a {@link CerberusServerException}
     * will be thrown with the code and error details.  If an unexpected I/O error is
     * encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     * <p>
     * See https://github.com/Nike-Inc/cerberus/blob/master/API.md for details on what the
     * list files operation returns.
     * </p>
     *
     * @param path   Path to the data
     * @param limit  The max number of results to return
     * @param offset The number offset of results to return
     * @return List of metadata for secure files at the specified path
     */
    public CerberusListFilesResponse listFiles(final String path, Integer limit, Integer offset) {
        final HttpUrl httpUrl = buildUrl("v1/secure-files/", path, limit, offset);

        LOGGER.debug("list: requestUrl={}, limit={}, offset={}", httpUrl, limit, offset);
        final Response response = ofSupplier(
                () -> execute(httpUrl, HttpMethod.GET, null)
        )
                .withRetry(RETRY)
                .decorate()
                .get();

        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, CerberusListFilesResponse.class);
    }

    /**
     * Read operation for a specified path.  Will return a {@link Map} of the data stored at the specified path.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @param path Path to the data
     * @return Map of the data
     */
    public CerberusResponse read(final String path) {
        return buildAndExecuteRequest(SECRET_PATH_PREFIX, path, HttpMethod.GET, null, CerberusResponse.class);
    }

    /**
     * Read the binary contents of the file at the specified path. Will return the file contents stored at the specified path.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @param path Path to the data
     * @return File contents
     */
    public byte[] readFileAsBytes(final String path) {
        Response response = buildAndExecuteRequest(SECURE_FILE_PATH_PREFIX, path, HttpMethod.GET, null);
        return responseBodyAsBytes(response);
    }

    /**
     * Write operation for a specified path and data set. If Cerberus returns an unexpected response code, a
     * {@link CerberusServerException} will be thrown with the code and error details.  If an unexpected I/O
     * error is encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     *
     * @param path Path for where to store the data
     * @param data Data to be stored
     */
    public void write(final String path, final Map<String, String> data) {
        buildAndExecuteRequest(SECRET_PATH_PREFIX, path, HttpMethod.POST, data);
    }

    /**
     * Write operation for file at specified path with given content. If Cerberus returns an unexpected response code, a
     * {@link CerberusServerException} will be thrown with the code and error details.  If an unexpected I/O
     * error is encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     *
     * @param path     Path for where to store the data
     * @param contents File contents to be stored
     */
    public void writeFile(final String path, final byte[] contents) {
        final String fileName = StringUtils.substringAfterLast(path, "/");
        final HttpUrl httpUrl = buildUrl(SECURE_FILE_PATH_PREFIX, path);
        LOGGER.debug("write: requestUrl={}", httpUrl);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file-content", fileName,
                        RequestBody.create(MediaType.parse("application/octet-stream"), contents))
                .build();

        Request request = new Request.Builder()
                .url(httpUrl)
                .headers(defaultHeaders)
                .addHeader(HttpHeader.CERBERUS_TOKEN, credentialsProvider.getCredentials().getToken())
                .addHeader(HttpHeader.ACCEPT, DEFAULT_MEDIA_TYPE.toString())
                .post(requestBody)
                .build();

        final Response response = execute(request);

        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }

    /**
     * Delete operation for a file path.  If Cerberus returns an unexpected response code, a
     * {@link CerberusServerException} will be thrown with the code and error details.  If an unexpected I/O
     * error is encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     *
     * @param path Path to file to be deleted
     */
    public void deleteFile(final String path) {
        buildAndExecuteRequest(SECURE_FILE_PATH_PREFIX, path, HttpMethod.DELETE, null);
    }

    /**
     * Delete operation for a specified path.  If Cerberus returns an unexpected response code, a
     * {@link CerberusServerException} will be thrown with the code and error details.  If an unexpected I/O
     * error is encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     *
     * @param path Path to data to be deleted
     */
    public void delete(final String path) {
        buildAndExecuteRequest(SECRET_PATH_PREFIX, path, HttpMethod.DELETE, null);
    }

    /**
     * Lists all roles that Cerberus supports.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @return List of all roles
     */
    public List<CerberusRoleResponse> listRoles() {
        return buildAndExecuteRequest(ROLE_PATH, EMPTY_PATH, HttpMethod.GET, null, new TypeToken<List<CerberusRoleResponse>>(){}.getType());
    }

    /**
     * Gets all roles that Cerberus supports in a more usable {@link CerberusRolePermission} to ID map.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @return Map of role permission to ID
     */
    public Map<CerberusRolePermission, String> getRolePermissionMap() {
        return listRoles().stream().collect(Collectors.toMap(roleResponse -> CerberusRolePermission.fromString(roleResponse.getName()), CerberusRoleResponse::getId));
    }

    /**
     * Lists all categories that Cerberus supports.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @return List of all categories
     */
    public List<CerberusCategoryResponse> listCategories() {
        return buildAndExecuteRequest(CATEGORY_PATH, EMPTY_PATH, HttpMethod.GET, null, new TypeToken<List<CerberusCategoryResponse>>(){}.getType());
    }

    /**
     * Gets all categories that Cerberus supports in a more usable category path to ID map.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @return Map of category path to ID
     */
    public Map<String, String> getCategoryMap() {
        return listCategories().stream().collect(Collectors.toMap(CerberusCategoryResponse::getPath, CerberusCategoryResponse::getId));
    }

    /**
     * Gets the category ID that matches the path.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @param path The path of category (e.g. "app")
     * @return Map of category path to ID
     */
    public String getCategoryIdByPath(String path) {
        return getCategoryMap().get(path);
    }

    /**
     * Lists all safe deposit boxes that the authenticated IAM principal has access to. If Cerberus returns an unexpected response code, a {@link CerberusServerException}
     * will be thrown with the code and error details.  If an unexpected I/O error is
     * encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     * <p>
     * See https://github.com/Nike-Inc/cerberus/blob/master/API.md for details on what the
     * list safe deposit box operation returns.
     * </p>
     *
     * @return List of safe deposit box summaries
     */
    public List<CerberusSafeDepositBoxSummaryResponse> listSafeDepositBoxes() {
        return buildAndExecuteRequest(SAFE_DEPOSIT_BOX_PREFIX, EMPTY_PATH, HttpMethod.GET, null, new TypeToken<List<CerberusSafeDepositBoxSummaryResponse>>(){}.getType());
    }


    /**
     * Gets the safe deposit box metadata by its name.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @param name The name of the safe deposit box
     * @return The safe deposit box metadata
     */
    public CerberusSafeDepositBoxResponse getSafeDepositBoxByName(String name) {
        List<CerberusSafeDepositBoxSummaryResponse> responses = listSafeDepositBoxes().stream()
                .filter(sdb -> sdb.getName().equals(name))
                .collect(Collectors.toList());
        if (responses.isEmpty()) {
            throw new CerberusClientException("ERROR cannot find safe deposit box with the name " + name);
        }
        return getSafeDepositBoxById(responses.get(0).getId());
    }

    /**
     * Gets the safe deposit box metadata by its ID.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @param id The ID of the safe deposit box
     * @return The safe deposit box metadata
     */
    public CerberusSafeDepositBoxResponse getSafeDepositBoxById(String id) {
        return buildAndExecuteRequest(SAFE_DEPOSIT_BOX_PREFIX, id, HttpMethod.GET, null, CerberusSafeDepositBoxResponse.class);
    }

    /**
     * Creates a safe deposit box.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @param cerberusSafeDepositBoxRequest The metadata of the safe deposit box
     * @return The metadata of the created safe deposit box
     */
    public CerberusSafeDepositBoxResponse createSafeDepositBox(CerberusSafeDepositBoxRequest cerberusSafeDepositBoxRequest) {
        return buildAndExecuteRequest(SAFE_DEPOSIT_BOX_PREFIX, EMPTY_PATH, HttpMethod.POST, cerberusSafeDepositBoxRequest, CerberusSafeDepositBoxResponse.class);
    }


    /**
     * Updates a safe deposit box.
     * If Cerberus returns an unexpected response code, a {@link CerberusServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link CerberusClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @param cerberusSafeDepositBoxRequest The metadata of the safe deposit box
     * @return The metadata of the updated safe deposit box
     */
    public CerberusSafeDepositBoxResponse updateSafeDepositBox(String id, CerberusSafeDepositBoxRequest cerberusSafeDepositBoxRequest) {
        return buildAndExecuteRequest(SAFE_DEPOSIT_BOX_PREFIX, id, HttpMethod.PUT, cerberusSafeDepositBoxRequest, CerberusSafeDepositBoxResponse.class);
    }

    /**
     * Deletes a safe deposit box.
     * If Cerberus returns an unexpected response code, a
     * {@link CerberusServerException} will be thrown with the code and error details.  If an unexpected I/O
     * error is encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     *
     * @param id ID of the safe deposit box to be deleted
     */
    public void deleteSafeDepositBox(String id) {
        buildAndExecuteRequest(SAFE_DEPOSIT_BOX_PREFIX, id, HttpMethod.DELETE, null);
    }

    /**
     * Returns a copy of the URL being used for communicating with Cerberus
     * @return Copy of the HttpUrl object
     */
    public HttpUrl getCerberusUrl() {
        return HttpUrl.parse(url);
    }

    /**
     * Returns the configured credentials provider.
     * @return The configured credentials provider
     */
    public CerberusCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    /**
     * Gets the Gson object used for serializing and de-serializing requests.
     * @return Gson object
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Returns the configured default HTTP headers.
     * @return The configured default HTTP headers
     */
    public Headers getDefaultHeaders() {
        return defaultHeaders;
    }

    /**
     * Builds the full URL for preforming an operation against Cerberus.
     *
     * @param prefix Prefix between the environment URL and specified path
     * @param path   Path for the requested operation
     * @param limit  Limit of items to return in a paginated call
     * @param offset Number offset of items in a paginated call
     * @return Full URL to execute a request against
     */
    protected HttpUrl buildUrl(final String prefix,
                               final String path,
                               final Integer limit,
                               final Integer offset) {
        String baseUrl = url;
        baseUrl = StringUtils.appendIfMissing(baseUrl, "/");

        final StringBuilder fullUrl = new StringBuilder()
                .append(baseUrl)
                .append(prefix)
                .append(path);

        if (limit != null && offset != null) {
             fullUrl.append("?limit=").append(limit).append("&offset=").append(offset);
        } else if (limit != null) {
            fullUrl.append("?limit=").append(limit);
        } else if (offset != null) {
            fullUrl.append("?offset=").append(offset);
        }

        return HttpUrl.parse(fullUrl.toString());
    }

    /**
     * Builds the full URL for preforming an operation against Cerberus.
     *
     * @param prefix Prefix between the environment URL and specified path
     * @param path   Path for the requested operation
     * @return Full URL to execute a request against
     */
    protected HttpUrl buildUrl(final String prefix, final String path) {
        String baseUrl = url;

        if (!StringUtils.endsWith(baseUrl, "/")) {
            baseUrl += "/";
        }

        return HttpUrl.parse(baseUrl + prefix + path);
    }

    protected <M> M buildAndExecuteRequest(final String prefix, final String path, final String httpMethod, Object requestBody, final Class<M> responseClass) {
        final Response response = buildAndExecuteRequest(prefix, path, httpMethod, requestBody);
        return parseResponseBody(response, responseClass);
    }

    protected <M> M buildAndExecuteRequest(final String prefix, final String path, final String httpMethod, Object requestBody, final Type typeOf) {
        final Response response = buildAndExecuteRequest(prefix, path, httpMethod, requestBody);
        return parseResponseBody(response, typeOf);
    }

    private Response buildAndExecuteRequest(final String prefix, final String path, final String httpMethod, Object requestBody) {
        final HttpUrl httpUrl = buildUrl(prefix, path);
        LOGGER.debug("requestUrl={}, HTTP method={}", httpUrl, httpMethod);

        final Response response = ofSupplier(
                () -> execute(httpUrl, httpMethod, requestBody)
        )
                .withRetry(RETRY)
                .decorate()
                .get();

        if (!response.isSuccessful()) {
            parseAndThrowApiErrorResponse(response);
        }

        return response;
    }

    /**
     * Executes the HTTP request based on the input parameters.
     *
     * @param httpUrl     The URL to execute the request against
     * @param method      The HTTP method for the request
     * @param requestBody The request body of the HTTP request
     * @return Response from the server
     */
    protected Response execute(final HttpUrl httpUrl, final String method, final Object requestBody) {
        try {
            Request request = buildRequest(httpUrl, method, requestBody);

            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            if (e instanceof SSLException
                    && e.getMessage() != null
                    && e.getMessage().contains("Unrecognized SSL message, plaintext connection?")) {
                throw new CerberusClientException("I/O error while communicating with Cerberus. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect", e);
            } else {
                throw new CerberusClientException("I/O error while communicating with Cerberus.", e);
            }
        }
    }

    /**
     * Executes the HTTP request based on the input parameters.
     *
     * @param request The HTTP request to be made
     * @return Response from the server
     */
    protected Response execute(final Request request) {
        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            if (e instanceof SSLException
                    && e.getMessage() != null
                    && e.getMessage().contains("Unrecognized SSL message, plaintext connection?")) {
                throw new CerberusClientException("I/O error while communicating with Cerberus. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect", e);
            } else {
                throw new CerberusClientException("I/O error while communicating with Cerberus.", e);
            }
        }
    }

    /**
     * Build the HTTP request to execute for the Cerberus Client
     * @param httpUrl     The URL to execute the request against
     * @param method      The HTTP method for the request
     * @param requestBody The request body of the HTTP request
     * @return - The HTTP request
     */
    protected Request buildRequest(final HttpUrl httpUrl, final String method, final Object requestBody) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(httpUrl)
                .headers(defaultHeaders)  // call headers method first because it overwrites all existing headers
                .addHeader(HttpHeader.CERBERUS_TOKEN, credentialsProvider.getCredentials().getToken())
                .addHeader(HttpHeader.ACCEPT, DEFAULT_MEDIA_TYPE.toString());

        if (requestBody != null) {
            requestBuilder.addHeader(HttpHeader.CONTENT_TYPE, DEFAULT_MEDIA_TYPE.toString())
                    .method(method, RequestBody.create(DEFAULT_MEDIA_TYPE, gson.toJson(requestBody)));
        } else {
            requestBuilder.method(method, null);
        }

        return requestBuilder.build();
    }


    /**
     * Convenience method for parsing the HTTP response and mapping it to a class.
     *
     * @param response      The HTTP response object
     * @param responseClass The class to map the response body to
     * @param <M>           Represents the type to map to
     * @return Deserialized object from the response body
     */
    protected <M> M parseResponseBody(final Response response, final Class<M> responseClass) {
        final String responseBodyStr = responseBodyAsString(response);
        try {
            return gson.fromJson(responseBodyStr, responseClass);
        } catch (JsonSyntaxException e) {
            LOGGER.error("parseResponseBody: responseCode={}, requestUrl={}, response={}",
                    response.code(), response.request().url(), responseBodyStr);
            throw new CerberusClientException("Error parsing the response body from Cerberus, response code: "
                + response.code() + ", response body: " + responseBodyStr, e);
        }
    }

    /**
     * Convenience method for parsing the HTTP response and mapping it to a type.
     *
     * @param response The HTTP response object
     * @param typeOf   The type to map the response body to
     * @param <M>      Represents the type to map to
     * @return Deserialized object from the response body
     */
    protected <M> M parseResponseBody(final Response response, final Type typeOf) {
        final String responseBodyStr = responseBodyAsString(response);
        try {
            return gson.fromJson(responseBodyStr, typeOf);
        } catch (JsonSyntaxException e) {
            LOGGER.error("parseResponseBody: responseCode={}, requestUrl={}, response={}",
                    response.code(), response.request().url(), responseBodyStr);
            throw new CerberusClientException("Error parsing the response body from Cerberus, response code: "
                + response.code() + ", response body: " + responseBodyStr, e);
        }
    }

    /**
     * Convenience method for parsing the errors from the HTTP response and throwing a {@link CerberusServerException}.
     *
     * @param response Response to parses the error details from
     */
    protected void parseAndThrowErrorResponse(final Response response) {
        final String responseBodyStr = responseBodyAsString(response);
        LOGGER.debug("parseAndThrowErrorResponse: responseCode={}, requestUrl={}, response={}",
                response.code(), response.request().url(), responseBodyStr);

        try {
            ErrorResponse errorResponse = gson.fromJson(responseBodyStr, ErrorResponse.class);

            if (errorResponse != null) {
                throw new CerberusServerException(response.code(), errorResponse.getErrors());
            } else {
                throw new CerberusServerException(response.code(), new LinkedList<String>());
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error("ERROR Failed to parse error message, response body received: {}", responseBodyStr);
            throw new CerberusClientException("Error parsing the error response body from Cerberus, response code: "
                + response.code() + ", response body: " + responseBodyStr, e);
        }
    }

    /**
     * Convenience method for parsing the errors from the HTTP response and throwing a {@link CerberusServerApiException}.
     *
     * @param response Response to parses the error details from
     */
    protected void parseAndThrowApiErrorResponse(final Response response) {
        final String responseBodyStr = responseBodyAsString(response);
        LOGGER.debug("parseAndThrowApiErrorResponse: responseCode={}, requestUrl={}, response={}",
                response.code(), response.request().url(), responseBodyStr);

        try {
            ApiErrorResponse errorResponse = gson.fromJson(responseBodyStr, ApiErrorResponse.class);

            if (errorResponse != null) {
                throw new CerberusServerApiException(response.code(), errorResponse.getErrorId(), errorResponse.getErrors());
            } else {
                throw new CerberusServerApiException(response.code(), null, new LinkedList<CerberusApiError>());
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error("ERROR Failed to parse error message, response body received: {}", responseBodyStr);
            throw new CerberusClientException("Error parsing the error response body from Cerberus, response code: "
                + response.code() + ", response body: " + responseBodyStr, e);
        }
    }

    /**
     * POJO for representing error response body from Cerberus.
     */
    protected static class ApiErrorResponse {
        private String errorId;
        private List<CerberusApiError> errors;

        public List<CerberusApiError> getErrors() {
            return errors;
        }

        public String getErrorId() {
            return errorId;
        }
    }

    /**
     * POJO for representing error response body from Cerberus.
     */
    protected static class ErrorResponse {
        private List<String> errors;

        public List<String> getErrors() {
            return errors;
        }
    }

    protected String responseBodyAsString(Response response) {
        try {
            return response.body().string();
        } catch (IOException ioe) {
            LOGGER.debug("responseBodyAsString: response={}", gson.toJson(response));
            return "ERROR failed to print response body as str: " + ioe.getMessage();
        }
    }

    protected byte[] responseBodyAsBytes(Response response) {
        try {
            return response.body().bytes();
        } catch (IOException ioe) {
            LOGGER.debug("responseBodyAsString: response={}", gson.toJson(response));
            throw new CerberusClientException("ERROR failed to print: " + response.toString());
        }
    }
}
