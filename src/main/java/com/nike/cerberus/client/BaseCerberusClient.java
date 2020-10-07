package com.nike.cerberus.client;

import static io.github.resilience4j.decorators.Decorators.ofSupplier;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSyntaxException;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.exception.CerberusApiError;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.exception.CerberusServerApiException;
import com.nike.cerberus.client.exception.CerberusServerException;
import com.nike.cerberus.client.model.error.ApiErrorResponse;
import com.nike.cerberus.client.model.error.ErrorResponse;
import com.nike.cerberus.client.model.http.HttpHeader;
import com.nike.cerberus.client.model.http.HttpMethod;
import com.nike.cerberus.client.model.http.HttpParam;

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

public abstract class BaseCerberusClient {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
	
	protected final int DEFAULT_OFFSET 		= 0;
	protected final int DEFAULT_LIMIT 		= 100;
	private final int DEFAULT_NUM_RETRIES 	= 3;

	private final RetryConfig RETRY_CONFIG = RetryConfig.<Response>custom().maxAttempts(DEFAULT_NUM_RETRIES)
			.retryOnResult(response -> response.code() >= 500 && response.code() <= 599)
			.intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.of(250, ChronoUnit.MILLIS))).build();
	private final Retry RETRY = Retry.of(this.getClass().getName(), RETRY_CONFIG);

	private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.disableHtmlEscaping().registerTypeAdapter(DateTime.class,
					(JsonDeserializer<DateTime>) (json, typeOfT, context) -> new DateTime(json.getAsString()))
			.create();

	private final CerberusCredentialsProvider credentialsProvider;
	private final OkHttpClient httpClient;
	private final String url;
	private final Headers defaultHeaders;

	/*
	 * Constructors
	 */

	public BaseCerberusClient(final String cerberusUrl, final CerberusCredentialsProvider credentialsProvider,
			final OkHttpClient httpClient, final Headers defaultHeaders) {

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

	public BaseCerberusClient(final String cerberusUrl, final CerberusCredentialsProvider credentialsProvider,
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

	/*
	 * Execute request
	 */
		
	protected Response executeWithRetry(HttpUrl httpUrl,HttpMethod method) {
		return executeWithRetry(httpUrl,method,null);
	}
	
	protected Response executeWithRetry(HttpUrl httpUrl,HttpMethod method,Object requestBody) {
		return ofSupplier(() -> execute(httpUrl, method, requestBody)).withRetry(RETRY).decorate().get();
	}

	
	protected Response execute(HttpUrl httpUrl, final byte[] contents) {
    	List<String> segments 	= httpUrl.pathSegments();
        String fileName 		= segments.get(segments.size() -1);

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

        return execute(request);
    }
	
	private Response execute(final HttpUrl httpUrl, final HttpMethod method, final Object requestBody) {
		return execute(buildRequest(httpUrl, method, requestBody));
	}
	
	private Response execute(Request request) {
		try {
			return httpClient.newCall(request).execute();
		} catch (IOException e) {
			if (e instanceof SSLException && e.getMessage() != null
					&& e.getMessage().contains("Unrecognized SSL message, plaintext connection?")) {
				throw new CerberusClientException(
						"I/O error while communicating with Cerberus. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect",
						e);
			} else {
				throw new CerberusClientException("I/O error while communicating with Cerberus.", e);
			}
		}
	}
	
	/*
	 * Build request
	 */

	protected Request buildRequest(final HttpUrl httpUrl, final HttpMethod method, final Object requestBody) {
		Request.Builder requestBuilder = new Request.Builder().url(httpUrl).headers(defaultHeaders) // call headers
																									// method first
																									// because it
																									// overwrites all
																									// existing headers
				.addHeader(HttpHeader.CERBERUS_TOKEN, credentialsProvider.getCredentials().getToken())
				.addHeader(HttpHeader.ACCEPT, DEFAULT_MEDIA_TYPE.toString());

		if (requestBody != null) {
			requestBuilder.addHeader(HttpHeader.CONTENT_TYPE, DEFAULT_MEDIA_TYPE.toString()).method(method.getHttpMethod(),
					RequestBody.create(DEFAULT_MEDIA_TYPE, gson.toJson(requestBody)));
		} else {
			requestBuilder.method(method.getHttpMethod(), null);
		}

		return requestBuilder.build();
	}
	
	/*
	 * Params
	 */
	protected Map<String,String> getLimitMappings(int limit, int offset){
		Map<String,String> mapping = new HashMap<>();
		if(limit > 0) {
			mapping.put(HttpParam.LIMIT, ""+limit);
		}
		if(offset > -1) {
			mapping.put(HttpParam.OFFSET, ""+offset);
		}
		return mapping;
	}

	/*
	 * Build urls
	 */
	
	protected HttpUrl buildUrl(String base, String... pathVariables) {
		return HttpUrl.parse(buildUrlwithPathVariables(base,null,pathVariables));
	}
	
	protected HttpUrl buildUrl(String base,Map<String,String> params, String... pathVariables) {
		return HttpUrl.parse(buildUrlwithPathVariables(base,params,pathVariables));
	}
	
	private String buildUrlwithPathVariables(String base,Map<String,String> params, String... pathVariables) {
		String baseUrl = StringUtils.appendIfMissing(url, "/") + base;
		for (String variable : pathVariables) {
			baseUrl = StringUtils.appendIfMissing(baseUrl, "/");
			baseUrl = baseUrl + variable;
		}
		if(params != null && !params.isEmpty()) {
			boolean firstParam = true;
			for (Entry<String, String> param : params.entrySet()) {
				if(firstParam) {
					baseUrl = StringUtils.appendIfMissing(baseUrl, "?");
				}else {
					baseUrl = StringUtils.appendIfMissing(baseUrl, "&");
				}
				baseUrl = baseUrl + param.getKey() + "=" + param.getValue();
				firstParam = false;
			}
		}
		return baseUrl;
	}

	/*
	 * Parsing
	 */

	protected String responseBodyAsString(Response response) {
		try {
			return response.body().string();
		} catch (IOException ioe) {
			logger.debug("responseBodyAsString: response={}", gson.toJson(response));
			return "ERROR failed to print response body as str: " + ioe.getMessage();
		}
	}

	protected byte[] responseBodyAsBytes(Response response) {
		try {
			return response.body().bytes();
		} catch (IOException ioe) {
			logger.debug("responseBodyAsString: response={}", gson.toJson(response));
			throw new CerberusClientException("ERROR failed to print: " + response.toString());
		}
	}

	protected void parseAndThrowApiErrorResponse(final Response response) {
		final String responseBodyStr = responseBodyAsString(response);
		logger.debug("parseAndThrowApiErrorResponse: responseCode={}, requestUrl={}, response={}", response.code(),
				response.request().url(), responseBodyStr);

		try {
			ApiErrorResponse errorResponse = gson.fromJson(responseBodyStr, ApiErrorResponse.class);

			if (errorResponse != null) {
				throw new CerberusServerApiException(response.code(), errorResponse.getErrorId(),
						errorResponse.getErrors());
			} else {
				throw new CerberusServerApiException(response.code(), null, new LinkedList<CerberusApiError>());
			}
		} catch (JsonSyntaxException e) {
			logger.error("ERROR Failed to parse error message, response body received: {}", responseBodyStr);
			throw new CerberusClientException("Error parsing the error response body from Cerberus, response code: "
					+ response.code() + ", response body: " + responseBodyStr, e);
		}
	}

	protected <M> M parseResponseBody(final Response response, final Type typeOf) {
		final String responseBodyStr = responseBodyAsString(response);
		try {
			return gson.fromJson(responseBodyStr, typeOf);
		} catch (JsonSyntaxException e) {
			logger.error("parseResponseBody: responseCode={}, requestUrl={}, response={}", response.code(),
					response.request().url(), responseBodyStr);
			throw new CerberusClientException("Error parsing the response body from Cerberus, response code: "
					+ response.code() + ", response body: " + responseBodyStr, e);
		}
	}

	protected void parseAndThrowErrorResponse(final Response response) {
		final String responseBodyStr = responseBodyAsString(response);
		logger.debug("parseAndThrowErrorResponse: responseCode={}, requestUrl={}, response={}", response.code(),
				response.request().url(), responseBodyStr);

		try {
			ErrorResponse errorResponse = gson.fromJson(responseBodyStr, ErrorResponse.class);

			if (errorResponse != null) {
				throw new CerberusServerException(response.code(), errorResponse.getErrors());
			} else {
				throw new CerberusServerException(response.code(), new LinkedList<String>());
			}
		} catch (JsonSyntaxException e) {
			logger.error("ERROR Failed to parse error message, response body received: {}", responseBodyStr);
			throw new CerberusClientException("Error parsing the error response body from Cerberus, response code: "
					+ response.code() + ", response body: " + responseBodyStr, e);
		}
	}

	protected <M> M parseResponseBody(final Response response, final Class<M> responseClass) {
		final String responseBodyStr = responseBodyAsString(response);
		try {
			return gson.fromJson(responseBodyStr, responseClass);
		} catch (JsonSyntaxException e) {
			logger.error("parseResponseBody: responseCode={}, requestUrl={}, response={}", response.code(),
					response.request().url(), responseBodyStr);
			throw new CerberusClientException("Error parsing the response body from Cerberus, response code: "
					+ response.code() + ", response body: " + responseBodyStr, e);
		}
	}

	// ########################## Returns #################################

	public Gson getGson() {
		return gson;
	}

	public HttpUrl getCerberusUrl() {
		return HttpUrl.parse(url);
	}

	public CerberusCredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public Headers getDefaultHeaders() {
		return defaultHeaders;
	}

}
