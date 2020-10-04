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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.model.AdminOverrideOwner;
import com.nike.cerberus.client.model.SDBCreated;
import com.nike.cerberus.client.model.SecureFileMetadata;
import com.nike.cerberus.client.model.http.HttpHeader;
import com.nike.cerberus.client.model.http.HttpMethod;
import com.nike.cerberus.client.model.http.HttpParam;
import com.nike.cerberus.client.model.http.HttpStatus;
import com.nike.cerberus.domain.AuthKmsKeyMetadataResult;
import com.nike.cerberus.domain.SafeDepositBoxSummary;
import com.nike.cerberus.domain.SafeDepositBoxV1;
import com.nike.cerberus.domain.SecureDataResponse;
import com.nike.cerberus.domain.SecureFileSummaryResult;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Client for interacting with a Cerberus.
 */
public class CerberusClient extends BaseCerberusClient{

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final String SAFE_DEPOSIT_BOX 		= "v1/safe-deposit-box";
	private static final String SECRETS 				= "v1/secret";
	private static final String SECURE_FILE				= "v1/secure-file";
	private static final String SECURE_FILES			= "v1/secure-files";
	
	private static final String ADMIN_AUTH_KMS_METADATA = "v1/admin/authentication-kms-metadata";
	private static final String ADMIN_OVERRIDE_OWNER 	= "v1/admin/override-owner";
	
	public CerberusClient(String cerberusUrl, CerberusCredentialsProvider credentialsProvider,OkHttpClient httpClient, Headers defaultHeaders) {
		super(cerberusUrl,credentialsProvider,httpClient,defaultHeaders);
	}
	
    public CerberusClient(String cerberusUrl, CerberusCredentialsProvider credentialsProvider,OkHttpClient httpClient) {
		super(cerberusUrl, credentialsProvider, httpClient);
	}
    
    /*
     * Safe deposit box
     */
    
    public List<SafeDepositBoxSummary> listSafeDepositBoxes() {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX);
        logger.debug("listSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return Arrays.asList(parseResponseBody(response, SafeDepositBoxSummary[].class));
    }
    
    public SDBCreated createSafeDepositBox(SafeDepositBoxV1 sdb) {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX);
        logger.debug("createSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.POST, sdb);
        if (response.code() == HttpStatus.CREATED) {
        	SDBCreated result = parseResponseBody(response, SDBCreated.class);
        	result.setLocation(response.header(HttpHeader.LOCATION));
        	return result;
        }else {
        	parseAndThrowApiErrorResponse(response);
        }
        return null;
    }
    
    public SafeDepositBoxV1 getSafeDepositBox(String id) {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX,id);
        logger.debug("getSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SafeDepositBoxV1.class);
    }
    
    public void updateSafeDepositBox(String id, SafeDepositBoxV1 sdb) {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX,id);
        logger.debug("updateSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.PUT, sdb);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    public void deleteSafeDepositBox(String id) {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX,id);
        logger.debug("deleteSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.DELETE);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    /*
     * Secrets
     */
    
    public SecureDataResponse listSecretPaths(String category, String sdbName, String path) {
		Map<String,String> mapping = new HashMap<>();
		mapping.put(HttpParam.LIST, "true");
		
        final HttpUrl httpUrl = buildUrl(SECRETS,mapping,category,sdbName,path);
        logger.debug("listSecretPaths: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SecureDataResponse.class);
    }
    
    /*
     * Secure-file
     */
    
    public byte[] getSecureFile(String path) {
        return getSecureFile(buildUrl(SECURE_FILE,path));
    }
    
    public SecureFileMetadata getSecureFileMetadata(String path) {
        final HttpUrl httpUrl = buildUrl(SECURE_FILE,path);
        logger.debug("getSecureFileMetadata: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.HEAD);
        if (response.code() == HttpStatus.OK) {
        	SecureFileMetadata metadata = new SecureFileMetadata();
        	metadata.setContentLength(Integer.parseInt(response.header(HttpHeader.CONTENT_LENGTH)));
        	
        	String disposition = response.header(HttpHeader.CONTENT_DISPOSITION);
        	metadata.setFilename(disposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1"));
        	return metadata;
        }else {
        	parseAndThrowApiErrorResponse(response);
        }

        return null;
    }
    
    public byte[] getSecureFile(String path, String versionId) {
    	Map<String,String> mapping = new HashMap<>();
		mapping.put(HttpParam.VERSION_ID, versionId);
        return getSecureFile(buildUrl(SECURE_FILE,mapping,path));
    }
    
    public void writeSecureFile(String path, final byte[] contents) {
        final HttpUrl httpUrl = buildUrl(SECURE_FILE, path);
        logger.debug("writeSecureFile: requestUrl={}", httpUrl);

        final Response response = execute(httpUrl, contents);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    public void deleteSecurefile(String path) {
        final HttpUrl httpUrl = buildUrl(SECURE_FILE,path);
        logger.debug("deleteSecurefile: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.DELETE);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    public SecureFileSummaryResult listSecureFiles(String category, String sdbName) {
    	return listSecureFiles(category,sdbName,DEFAULT_LIMIT,DEFAULT_OFFSET);
    }
    
    public SecureFileSummaryResult listSecureFiles(String category, String sdbName, int limit, int offset) {
    	Map<String,String> mapping = new HashMap<>();
		mapping.put(HttpParam.LIMIT, ""+limit);
		mapping.put(HttpParam.OFFSET, ""+offset);
    	
        final HttpUrl httpUrl = buildUrl(SECURE_FILES,mapping,category,sdbName);
        logger.debug("listSecureFiles: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SecureFileSummaryResult.class);
    }
    
    private byte[] getSecureFile(HttpUrl httpUrl) {
        logger.debug("getSecureFile: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return responseBodyAsBytes(response);
    }
    
    /*
     * Admin
     */
    
    public AuthKmsKeyMetadataResult adminGetAuthenticationKmsMetadata() {
        final HttpUrl httpUrl = buildUrl(ADMIN_AUTH_KMS_METADATA);
        logger.debug("adminGetAuthenticationKmsMetadata: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, AuthKmsKeyMetadataResult.class);
    }
    
    public void adminOverrideOwner(AdminOverrideOwner override) {
        final HttpUrl httpUrl = buildUrl(ADMIN_OVERRIDE_OWNER);
        logger.debug("adminOverrideOwner: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.PUT,override);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }

   
}
