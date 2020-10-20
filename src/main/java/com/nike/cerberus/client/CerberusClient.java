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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.domain.AuthKmsKeyMetadataResult;
import com.nike.cerberus.client.domain.Category;
import com.nike.cerberus.client.domain.Role;
import com.nike.cerberus.client.domain.SDBMetadataResult;
import com.nike.cerberus.client.domain.SafeDepositBoxSummary;
import com.nike.cerberus.client.domain.SafeDepositBoxV1;
import com.nike.cerberus.client.domain.SecureDataResponse;
import com.nike.cerberus.client.domain.SecureDataVersionsResult;
import com.nike.cerberus.client.domain.SecureFileSummaryResult;
import com.nike.cerberus.client.model.AdminOverrideOwner;
import com.nike.cerberus.client.model.CerberusListFilesResponse;
import com.nike.cerberus.client.model.CerberusListResponse;
import com.nike.cerberus.client.model.CerberusResponse;
import com.nike.cerberus.client.model.SDBCreated;
import com.nike.cerberus.client.model.SecureFileMetadata;
import com.nike.cerberus.client.model.http.HttpHeader;
import com.nike.cerberus.client.model.http.HttpMethod;
import com.nike.cerberus.client.model.http.HttpParam;
import com.nike.cerberus.client.model.http.HttpStatus;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Client for interacting with a Cerberus.
 */
public class CerberusClient extends BaseCerberusClient{

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final String SAFE_DEPOSIT_BOX 			= "v1/safe-deposit-box";
	private static final String SECRET 						= "v1/secret";
	private static final String SECURE_FILE					= "v1/secure-file";
	private static final String SECURE_FILES				= "v1/secure-files";
	private static final String METADATA					= "v1/metadata";
	private static final String ROLE						= "v1/role";
	private static final String CATEGORY					= "v1/category";
	private static final String SECRET_VERSIONS				= "v1/secret-versions";
	private static final String SDB_SECRET_VERSION_PATHS	= "v1/sdb-secret-version-paths";
	private static final String ADMIN_AUTH_KMS_METADATA 	= "v1/admin/authentication-kms-metadata";
	private static final String ADMIN_OVERRIDE_OWNER 		= "v1/admin/override-owner";
	
	public CerberusClient(String cerberusUrl, CerberusCredentialsProvider credentialsProvider,OkHttpClient httpClient, Headers defaultHeaders) {
		super(cerberusUrl,credentialsProvider,httpClient,defaultHeaders);
	}
	
    public CerberusClient(String cerberusUrl, CerberusCredentialsProvider credentialsProvider,OkHttpClient httpClient) {
		super(cerberusUrl, credentialsProvider, httpClient);
	}
    
    /*
     * Deprecated old interface
     */
    
    /**
     * @deprecated  replaced by {@link #getSecret(String category, String sdbName, String path)}
     */
    @Deprecated 
    public CerberusListResponse list(final String path) {
        final HttpUrl httpUrl = buildUrl(SECRET,path);
        logger.debug("list: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() == HttpStatus.NOT_FOUND) {
            response.close();
            return new CerberusListResponse();
        } else if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        SecureDataResponse responce = parseResponseBody(response, SecureDataResponse.class);
        CerberusListResponse output = new CerberusListResponse();
        @SuppressWarnings("unchecked")
		Map<String, Object> mapping = (Map<String, Object>) responce.getData();
        
        ArrayList<String> inputList = new ArrayList<String>();
        for (Object value : mapping.values()) {
        	if(value instanceof Collection) {
        		@SuppressWarnings("rawtypes")
				Collection innerCollection = (Collection) value;
        		for (Object innerValue : innerCollection) {
        			inputList.add(""+innerValue);
        		}
        	}else {
        		inputList.add(""+value);
        	}
		}
        output.setKeys(inputList);
        return output;
    }
    
    /**
     * @deprecated  replaced by {@link #listSecureFiles(String category, String sdbName)}
     */
    @Deprecated 
    public CerberusListFilesResponse listFiles(final String path) {
    	return listFiles(path, null, null);
    }
    
    /**
     * @deprecated  replaced by {@link #listSecureFiles(String category, String sdbName, int limit, int offset)}
     */
    @Deprecated 
    public CerberusListFilesResponse listFiles(final String path, Integer limit, Integer offset) {
    	Map<String,String> mapping = getLimitMappings(limit, offset);
    	
        final HttpUrl httpUrl = buildUrl(SECURE_FILES,mapping,path);
        logger.debug("listFiles: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, CerberusListFilesResponse.class);
    }
    
    /**
     * @deprecated  replaced by {@link #getSecret(String category, String sdbName, String path)}
     */
    @Deprecated 
    public CerberusResponse read(final String path) {
		final HttpUrl httpUrl = buildUrl(SECRET,path);
	    logger.debug("read: requestUrl={}", httpUrl);
	
	    final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
	    if (response.code() != HttpStatus.OK) {
	          parseAndThrowApiErrorResponse(response);
	    }
	
	    return parseResponseBody(response, CerberusResponse.class);
    }
    
    /**
     * @deprecated  replaced by {@link #getSecureFile(String category, String sdbName, String path)}
     */
    @Deprecated 
    public byte[] readFileAsBytes(final String path) {
    	return getSecureFile(buildUrl(SECURE_FILE,path));
    }
    
    /**
     * @deprecated  replaced by {@link #writeSecureFile(String category, String sdbName, String path, final byte[] contents)}
     */
    @Deprecated 
    public void writeFile(final String path, final byte[] contents) {
        final HttpUrl httpUrl = buildUrl(SECURE_FILE, path);
        logger.debug("writeFile: requestUrl={}", httpUrl);

        final Response response = execute(httpUrl, contents);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    /**
     * @deprecated  replaced by {@link #createSecret(String category, String sdbName, String path, Map<String,String> values)}
     */
    @Deprecated 
    public void write(final String path, final Map<String, String> values) {
        final HttpUrl httpUrl = buildUrl(SECRET,path);
        logger.debug("write: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.POST,values);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    /**
     * @deprecated  replaced by {@link #deleteSecurefile(String category, String sdbName, String path)}
     */
    @Deprecated 
    public void deleteFile(final String path) {
        final HttpUrl httpUrl = buildUrl(SECURE_FILE,path);
        logger.debug("deleteFile: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.DELETE);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    /**
     * @deprecated  replaced by {@link #deleteSecret(String category, String sdbName, String path)}
     */
    @Deprecated 
    public void delete(final String path) {
        final HttpUrl httpUrl = buildUrl(SECRET,path);
        logger.debug("delete: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.DELETE);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    // ------------------------------------------------------------------------------------------------------------
    
    /*
     * Safe deposit box
     */
    
    public List<SafeDepositBoxSummary> getSafeDepositBoxes() {
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX);
        logger.debug("listSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return Arrays.asList(parseResponseBody(response, SafeDepositBoxSummary[].class));
    }
    
    public SDBCreated createSafeDepositBox(SafeDepositBoxV1 sdb) {
    	checkForNull("sdb", sdb);
    	
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
    	checkForNull("id", id);
    	
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX,id);
        logger.debug("getSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SafeDepositBoxV1.class);
    }
    
    public void updateSafeDepositBox(String id, SafeDepositBoxV1 sdb) {
    	checkForNull("id", id);
    	checkForNull("sdb", sdb);
    	
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX,id);
        logger.debug("updateSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.PUT, sdb);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    public void deleteSafeDepositBox(String id) {
    	checkForNull("id", id);
    	
        final HttpUrl httpUrl = buildUrl(SAFE_DEPOSIT_BOX,id);
        logger.debug("deleteSafeDepositBox: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.DELETE);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    /*
     * Secret-versions
     */
    
    public SecureDataVersionsResult getVersionPathsForSdb(String category, String sdbName, String path) {
    	return getVersionPathsForSdb(category, sdbName, path,100,0);
    }
    
    public SecureDataVersionsResult getVersionPathsForSdb(String category, String sdbName, String path, int limit, int offset) {
    	checkForNull("category", category);
    	checkForNull("sdbName", sdbName);
    	checkForNull("path", path);
    	
    	Map<String,String> mapping = getLimitMappings(limit, offset);
    	
        final HttpUrl httpUrl = buildUrl(SECRET_VERSIONS,mapping,category,sdbName);
        logger.debug("getVersionPathsForSdb: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SecureDataVersionsResult.class);
    }
    
    /*
     * SDB secret version paths
     */
    
    @SuppressWarnings("unchecked")
	public Set<String> getSdbSecretVersionPaths(String sdbId){
        final HttpUrl httpUrl = buildUrl(SDB_SECRET_VERSION_PATHS,sdbId);
        logger.debug("getSdbSecretVersionPaths: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, Set.class);
    }
    
    /*
     * Role
     */
    
    public List<Role> getRoles(){
        final HttpUrl httpUrl = buildUrl(ROLE);
        logger.debug("getRoles: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return Arrays.asList(parseResponseBody(response, Role[].class));
    }
    
    public Role getRole(String roleId){
    	checkForNull("roleId", roleId);
    	
        final HttpUrl httpUrl = buildUrl(ROLE,roleId);
        logger.debug("getRoles: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, Role.class);
    }
    
    /*
     * Category
     */
    
    public List<Category> getCategories(){
        final HttpUrl httpUrl = buildUrl(CATEGORY);
        logger.debug("getRoles: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return Arrays.asList(parseResponseBody(response, Category[].class));
    }
    
    public Category getCategory(String categoryId){
    	checkForNull("categoryId", categoryId);
    	
        final HttpUrl httpUrl = buildUrl(CATEGORY,categoryId);
        logger.debug("getRoles: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, Category.class);
    }
    
    public void deleteCategory(String categoryId){
    	checkForNull("categoryId", categoryId);
    	
        final HttpUrl httpUrl = buildUrl(CATEGORY,categoryId);
        logger.debug("getRoles: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.DELETE);
        if (response.code() != HttpStatus.OK && response.code() != HttpStatus.NOT_FOUND) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    public Category createCategory(Category category){
    	checkForNull("category", category);
    	
        final HttpUrl httpUrl = buildUrl(CATEGORY);
        logger.debug("getRoles: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.POST,category);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, Category.class);
    }
    
    /*
     * Secrets
     */
    
    public SecureDataResponse getSecret(String category, String sdbName, String path) {
    	Map<String,String> mapping = new HashMap<>();
    	if(path != null && path.endsWith("/")) {
    		mapping.put(HttpParam.LIST, "true");
    	}
    	return getSecrets(mapping, category, sdbName, path);
    }

    public SecureDataResponse getSecretVersion(String category, String sdbName, String path, String versionId) {
    	Map<String,String> mapping = new HashMap<>();
    	mapping.put(HttpParam.VERSION_ID, versionId);
    	return getSecrets(mapping, category, sdbName, path);
    }
    
    public void createSecret(String category, String sdbName, String path, Map<String,String> values) {
        final HttpUrl httpUrl = buildUrl(SECRET,category,sdbName,path);
        logger.debug("getSecrets: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.POST,values);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    public void updateSecret(String category, String sdbName, String path, Map<String,String> values) {
        final HttpUrl httpUrl = buildUrl(SECRET,category,sdbName,path);
        logger.debug("getSecrets: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.PUT,values);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    public void deleteSecret(String category, String sdbName, String path) {
        final HttpUrl httpUrl = buildUrl(SECRET,category,sdbName,path);
        logger.debug("getSecrets: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.DELETE);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    private SecureDataResponse getSecrets(Map<String,String> mapping,String category, String sdbName, String path) {
        final HttpUrl httpUrl = buildUrl(SECRET,mapping,category,sdbName,path);
        logger.debug("getSecrets: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() == HttpStatus.NOT_FOUND) {
            response.close();
            return new SecureDataResponse();
        } else if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SecureDataResponse.class);
    }
    
    /*
     * Secure-file
     */
    
    public byte[] getSecureFile(String category, String sdbName, String path) {
        return getSecureFile(buildUrl(SECURE_FILE,category, sdbName, path));
    }
    
    public SecureFileMetadata getSecureFileMetadata(String category, String sdbName, String path) {
        final HttpUrl httpUrl = buildUrl(SECURE_FILE,category,sdbName,path);
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
    
    public byte[] getSecureFile(String category, String sdbName, String path, String versionId) {
    	Map<String,String> mapping = new HashMap<>();
		mapping.put(HttpParam.VERSION_ID, versionId);
        return getSecureFile(buildUrl(SECURE_FILE,mapping,category,sdbName,path));
    }
    
    public void writeSecureFile(String category, String sdbName, String path, final byte[] contents) {
        final HttpUrl httpUrl = buildUrl(SECURE_FILE, category,sdbName,path);
        logger.debug("writeSecureFile: requestUrl={}", httpUrl);

        final Response response = execute(httpUrl, contents);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }
    
    public void deleteSecurefile(String category, String sdbName, String path) {
        final HttpUrl httpUrl = buildUrl(SECURE_FILE,category,sdbName,path);
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
    	Map<String,String> mapping = getLimitMappings(limit, offset);
    	
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
     * Metadata
     */
    
    public SDBMetadataResult getMetadata() {
    	return getMetadata(null, DEFAULT_LIMIT, DEFAULT_OFFSET);
    }
    
    public SDBMetadataResult getMetadata(String sdbName) {
    	return getMetadata(sdbName, DEFAULT_LIMIT, DEFAULT_OFFSET);
    }
    
    public SDBMetadataResult getMetadata(String sdbName, int limit, int offset) {
    	Map<String,String> mapping = getLimitMappings(limit, offset);
    	if(sdbName != null) {
    		mapping.put(HttpParam.SDB_NAME, sdbName);
    	}
    	
        final HttpUrl httpUrl = buildUrl(METADATA);
        logger.debug("getMetadata: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, SDBMetadataResult.class);
    }
    
    /*
     * Admin
     */
    
    public AuthKmsKeyMetadataResult getAuthenticationKmsMetadata() {
        final HttpUrl httpUrl = buildUrl(ADMIN_AUTH_KMS_METADATA);
        logger.debug("adminGetAuthenticationKmsMetadata: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.GET);
        if (response.code() != HttpStatus.OK) {
            parseAndThrowApiErrorResponse(response);
        }

        return parseResponseBody(response, AuthKmsKeyMetadataResult.class);
    }
    
    public void overrideOwner(AdminOverrideOwner adminOverrideOwner) {
    	checkForNull("override", adminOverrideOwner);
    	
        final HttpUrl httpUrl = buildUrl(ADMIN_OVERRIDE_OWNER);
        logger.debug("adminOverrideOwner: requestUrl={}", httpUrl);

        final Response response = executeWithRetry(httpUrl, HttpMethod.PUT,adminOverrideOwner);
        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowApiErrorResponse(response);
        }
    }

   
}
