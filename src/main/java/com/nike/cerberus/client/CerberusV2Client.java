package com.nike.cerberus.client;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.model.http.HttpMethod;
import com.nike.cerberus.client.model.http.HttpStatus;
import com.nike.cerberus.client.domain.SafeDepositBoxSummary;
import com.nike.cerberus.client.domain.SafeDepositBoxV2;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;


/**
 * Client for interacting with a Cerberus.
 */
public class CerberusV2Client extends CerberusClient{

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final String SAFE_DEPOSIT_BOX_V2	= "v2/safe-deposit-box";
	
	public CerberusV2Client(String cerberusUrl, CerberusCredentialsProvider credentialsProvider,OkHttpClient httpClient, Headers defaultHeaders) {
		super(cerberusUrl,credentialsProvider,httpClient,defaultHeaders);
	}
	
    public CerberusV2Client(String cerberusUrl, CerberusCredentialsProvider credentialsProvider,OkHttpClient httpClient) {
		super(cerberusUrl, credentialsProvider, httpClient);
	}
    
    public List<SafeDepositBoxSummary> listSafeDepositBoxesV2() {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX_V2);
        logger.debug("listSafeDepositBoxesV2: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return Arrays.asList(parseResponseBody(response, SafeDepositBoxSummary[].class));
    }
    
    public SafeDepositBoxV2 createSafeDepositBoxV2(SafeDepositBoxV2 sdb) {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX_V2);
        logger.debug("createSafeDepositBoxV2: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.POST, sdb);
        if (response.code() != HttpStatus.CREATED) {
        	  parseAndThrowApiErrorResponse(response);
        }
        return parseResponseBody(response, SafeDepositBoxV2.class);
    }
    
    public SafeDepositBoxV2 getSafeDepositBoxV2(String id) {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX_V2,id);
        logger.debug("getSafeDepositBoxV2: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SafeDepositBoxV2.class);
    }
    
    public SafeDepositBoxV2 updateSafeDepositBoxV2(String id, SafeDepositBoxV2 sdb) {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX_V2,id);
        logger.debug("updateSafeDepositBoxV2: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.PUT, sdb);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SafeDepositBoxV2.class);
    }
    
    public void deleteSafeDepositBoxV2(String id) {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX_V2,id);
        logger.debug("deleteSafeDepositBoxV2: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.DELETE);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
}
