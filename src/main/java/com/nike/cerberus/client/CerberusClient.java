package com.nike.cerberus.client;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.http.HttpHeader;
import com.nike.cerberus.client.http.HttpMethod;
import com.nike.cerberus.client.http.HttpStatus;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Client for interacting with a Cerberus.
 */
public class CerberusClient {

    public static final String SECRET_PATH_PREFIX = "v1/secret/";

    public static final String AUTH_PATH_PREFIX = "v1/auth/";

    public static final String FILE_PATH_PREFIX = "v1/secure-file";

    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final CerberusCredentialsProvider credentialsProvider;

    private final OkHttpClient httpClient;

    private final UrlResolver urlResolver;

    private final Headers defaultHeaders;

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .create();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public CerberusClient(final UrlResolver cerberusUrlResolver,
                          final CerberusCredentialsProvider credentialsProvider,
                          final OkHttpClient httpClient,
                          final Headers defaultHeaders) {
        if (cerberusUrlResolver == null) {
            throw new IllegalArgumentException("Cerberus URL resolver cannot be null.");
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

        this.urlResolver = cerberusUrlResolver;
        this.credentialsProvider = credentialsProvider;
        this.httpClient = httpClient;
        this.defaultHeaders = defaultHeaders;
    }

    /**
     * Explicit constructor that allows for full control over construction of the Cerberus client.
     *
     * @param cerberusUrlResolver  URL resolver for Cerberus
     * @param credentialsProvider  Credential provider for acquiring a token for interacting with Cerberus
     * @param httpClient           HTTP client for calling Cerberus
     */
    public CerberusClient(final UrlResolver cerberusUrlResolver,
                          final CerberusCredentialsProvider credentialsProvider,
                          final OkHttpClient httpClient) {
        if (cerberusUrlResolver == null) {
            throw new IllegalArgumentException("Cerberus URL resolver can not be null.");
        }

        if (credentialsProvider == null) {
            throw new IllegalArgumentException("Credentials provider can not be null.");
        }

        if (httpClient == null) {
            throw new IllegalArgumentException("Http client can not be null.");
        }

        this.urlResolver = cerberusUrlResolver;
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
        final HttpUrl url = buildUrl(SECRET_PATH_PREFIX, path + "?list=true");
        logger.debug("list: requestUrl={}", url);

        final Response response = execute(url, HttpMethod.GET, null);

        if (response.code() == HttpStatus.NOT_FOUND) {
            response.close();
            return new CerberusListResponse();
        } else if (response.code() != HttpStatus.OK) {
            parseAndThrowErrorResponse(response);
        }

        final Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        final Map<String, Object> rootData = parseResponseBody(response, mapType);
        return gson.fromJson(gson.toJson(rootData.get("data")), CerberusListResponse.class);
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
        final HttpUrl url = buildUrl(SECRET_PATH_PREFIX, path);
        logger.debug("read: requestUrl={}", url);

        final Response response = execute(url, HttpMethod.GET, null);

        if (response.code() != HttpStatus.OK) {
            parseAndThrowErrorResponse(response);
        }

        return parseResponseBody(response, CerberusResponse.class);
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
        final HttpUrl url = buildUrl(SECRET_PATH_PREFIX, path);
        logger.debug("write: requestUrl={}", url);

        final Response response = execute(url, HttpMethod.POST, data);

        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowErrorResponse(response);
        }
    }

    /**
     * Delete operation for a specified path.  If Cerberus returns an unexpected response code, a
     * {@link CerberusServerException} will be thrown with the code and error details.  If an unexpected I/O
     * error is encountered, a {@link CerberusClientException} will be thrown wrapping the underlying exception.
     *
     * @param path Path to data to be deleted
     */
    public void delete(final String path) {
        final HttpUrl url = buildUrl(SECRET_PATH_PREFIX, path);
        logger.debug("delete: requestUrl={}", url);

        final Response response = execute(url, HttpMethod.DELETE, null);

        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowErrorResponse(response);
        }
    }

    /**
     * Returns a copy of the URL being used for communicating with Cerberus
     *
     * @return Copy of the HttpUrl object
     */
    public HttpUrl getCerberusUrl() {
        return HttpUrl.parse(urlResolver.resolve());
    }

    /**
     * Returns the configured credentials provider.
     *
     * @return The configured credentials provider
     */
    public CerberusCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    /**
     * Gets the Gson object used for serializing and de-serializing requests.
     *
     * @return Gson object
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Returns the configured default HTTP headers.
     *
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
     * @return Full URL to execute a request against
     */
    protected HttpUrl buildUrl(final String prefix, final String path) {
        String baseUrl = urlResolver.resolve();

        if (!StringUtils.endsWith(baseUrl, "/")) {
            baseUrl += "/";
        }

        return HttpUrl.parse(baseUrl + prefix + path);
    }

    /**
     * Executes the HTTP request based on the input parameters.
     *
     * @param url         The URL to execute the request against
     * @param method      The HTTP method for the request
     * @param requestBody The request body of the HTTP request
     * @return Response from the server
     */
    protected Response execute(final HttpUrl url, final String method, final Object requestBody) {
        try {
            Request request = buildRequest(url, method, requestBody);

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
     * @param url         The URL to execute the request against
     * @param method      The HTTP method for the request
     * @param requestBody The request body of the HTTP request
     * @return - The HTTP request
     */
    protected Request buildRequest(final HttpUrl url, final String method, final Object requestBody) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
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
            logger.error("parseResponseBody: responseCode={}, requestUrl={}, response={}",
                    response.code(), response.request().url(), responseBodyStr);
            throw new CerberusClientException("Error parsing the response body from Cerberus, response code: " + response.code(), e);
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
            logger.error("parseResponseBody: responseCode={}, requestUrl={}, response={}",
                    response.code(), response.request().url(), responseBodyStr);
            throw new CerberusClientException("Error parsing the response body from Cerberus, response code: " + response.code(), e);
        }
    }

    /**
     * Convenience method for parsing the errors from the HTTP response and throwing a {@link CerberusServerException}.
     *
     * @param response Response to parses the error details from
     */
    protected void parseAndThrowErrorResponse(final Response response) {
        final String responseBodyStr = responseBodyAsString(response);
        logger.debug("parseAndThrowErrorResponse: responseCode={}, requestUrl={}, response={}",
                response.code(), response.request().url(), responseBodyStr);

        try {
            ErrorResponse errorResponse = gson.fromJson(responseBodyStr, ErrorResponse.class);

            if (errorResponse != null) {
                throw new CerberusServerException(response.code(), errorResponse.getErrors());
            } else {
                throw new CerberusServerException(response.code(), new LinkedList<String>());
            }
        } catch (JsonSyntaxException e) {
            logger.error("ERROR Failed to parse error message, response body received: {}", responseBodyStr);
            throw new CerberusClientException("Error parsing the error response body from Cerberus, response code: " + response.code(), e);
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
            logger.debug("responseBodyAsString: response={}", gson.toJson(response));
            return "ERROR failed to print response body as str: " + ioe.getMessage();
        }
    }
}
