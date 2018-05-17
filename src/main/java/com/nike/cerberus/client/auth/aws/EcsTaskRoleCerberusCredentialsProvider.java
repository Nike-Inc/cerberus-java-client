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

import com.amazonaws.AmazonClientException;
import com.amazonaws.internal.EC2CredentialsUtils;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonSyntaxException;
import com.nike.cerberus.client.CerberusClientException;
import com.nike.cerberus.client.UrlResolver;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * {@link CerberusCredentialsProvider} implementation that uses the assigned role
 * to an ECS task to authenticate with Cerberus and decrypt the auth
 * response using KMS. If the assigned role has been granted the appropriate
 * provisioned for usage of Cerberus, it will succeed and have a token that can be
 * used to interact with Cerberus.
 * <p>
 * This class uses the AWS Task Metadata endpoint to look-up information automatically.
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-metadata-endpoint.html">Amazon ECS Task Metadata Endpoint</a>
 */
public class EcsTaskRoleCerberusCredentialsProvider extends BaseAwsCredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EcsTaskRoleCerberusCredentialsProvider.class);

    private static final Pattern TASK_ARN_PATTERN = Pattern.compile("arn:aws:ecs:(?<region>.*):(.*):task/(.*)");

    /** The name of the Json Object that contains the role ARN.*/
    final String ROLE_ARN = "RoleArn";

    /** Environment variable to get the Amazon ECS credentials resource path. */
    private static final String ECS_CONTAINER_CREDENTIALS_PATH = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";

    private static final String ECS_TASK_METADATA_RELATIVE_URI = "/v2/metadata";

    /** Default endpoint to retrieve the Amazon ECS Credentials and metadata. */
    private static final String ECS_CREDENTIALS_ENDPOINT = "http://169.254.170.2";

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     */
    public EcsTaskRoleCerberusCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver} and {@link OkHttpClient}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     * @param httpClient the client for interacting with Cerberus
     */
    public EcsTaskRoleCerberusCredentialsProvider(UrlResolver urlResolver, OkHttpClient httpClient) {
        super(urlResolver, httpClient);
    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver             Resolver for resolving the Cerberus URL
     * @param xCerberusClientOverride Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public EcsTaskRoleCerberusCredentialsProvider(UrlResolver urlResolver, String xCerberusClientOverride) {
        super(urlResolver, xCerberusClientOverride);
    }

    /**
     * Looks up the IAM roles assigned to the task via the ECS task metadata
     * service. An attempt is made to authenticate and decrypt the Cerberus
     * auth response with KMS using the task execution role. If successful,
     * the token retrieved is cached locally for future calls to
     * {@link BaseAwsCredentialsProvider#getCredentials()}.
     */
    @Override
    protected void authenticate() {
        String roleArn = getRoleArn();
        Region region = getRegion();

        try {
            getAndSetToken(roleArn, region);
            return;
        } catch (AmazonClientException ace) {
            LOGGER.warn("Unexpected error communicating with AWS services.", ace);
        } catch (JsonSyntaxException jse) {
            LOGGER.error("The decrypted auth response was not in the expected format!", jse);
        } catch (CerberusClientException sce) {
            LOGGER.warn("Unable to acquire Cerberus token for IAM role: " + roleArn, sce);
        }

        throw new CerberusClientException("Unable to acquire token with ECS task execution role.");
    }

    private String getRoleArn(){
        JsonNode node;
        JsonNode roleArn;
        try {
            String credentialsResponse = EC2CredentialsUtils.getInstance().readResource(
                    getCredentialsEndpoint());

            node = Jackson.jsonNodeOf(credentialsResponse);
            roleArn = node.get(ROLE_ARN);
            if (roleArn == null){
                throw new CerberusClientException("Task execution role ARN not found in task credentials.");
            }
            return roleArn.asText();
        } catch (JsonMappingException e) {
            LOGGER.error("Unable to parse response returned from service endpoint", e);
        } catch (IOException e) {
            LOGGER.error("Unable to load credentials from service endpoint", e);
        } catch (AmazonClientException ace) {
            LOGGER.warn("Unexpected error communicating with AWS services.", ace);
        }
        throw new CerberusClientException("Unable to find task execution role ARN.");
    }

    private Region getRegion(){
        try {
            String credentialsResponse = EC2CredentialsUtils.getInstance().readResource(getMetadataEndpoint());
            JsonNode node = Jackson.jsonNodeOf(credentialsResponse);
            JsonNode taskArn = node.get("TaskARN");
            final Matcher matcher = TASK_ARN_PATTERN.matcher(taskArn.asText());

            if (matcher.matches()) {
                final String region = matcher.group("region");
                if (StringUtils.isNotBlank(region)) {
                    return Region.getRegion(Regions.fromName(region));
                } else {
                    LOGGER.warn("Cannot parse region from task ARN {}", taskArn.asText());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to read resource from the task metadata endpoint.", e);
        } catch (URISyntaxException e) {
            LOGGER.warn(ECS_CREDENTIALS_ENDPOINT + ECS_TASK_METADATA_RELATIVE_URI + " could not be parsed as a URI reference.");
        } catch (RuntimeException e) {
            LOGGER.warn("Region lookup failed", e);
        }
        LOGGER.info("Using default region as fallback.");
        return Region.getRegion(Regions.DEFAULT_REGION);
    }

    private URI getMetadataEndpoint() throws URISyntaxException {
        return new URI(ECS_CREDENTIALS_ENDPOINT + ECS_TASK_METADATA_RELATIVE_URI);
    }


    private URI getCredentialsEndpoint(){
        String path = System.getenv(ECS_CONTAINER_CREDENTIALS_PATH);
        if (path == null) {
            throw new CerberusClientException("The environment variable " + ECS_CONTAINER_CREDENTIALS_PATH + " is empty");
        }
        try {
            return new URI(ECS_CREDENTIALS_ENDPOINT + path);
        } catch (URISyntaxException e) {
            throw new CerberusClientException(ECS_CREDENTIALS_ENDPOINT + path + " could not be parsed as a URI reference.");
        }
    }
}
