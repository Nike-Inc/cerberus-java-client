/*
 * Copyright (c) 2017 Nike, Inc.
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
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.gson.JsonSyntaxException;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.auth.VaultCredentialsProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nike.cerberus.client.auth.aws.StaticIamRoleVaultCredentialsProvider.IAM_ROLE_ARN_FORMAT;


/**
 * {@link VaultCredentialsProvider} implementation that uses the assigned role
 * to an EC2 instance to authenticate with Cerberus and decrypt the auth
 * response using KMS. If the assigned role has been granted the appropriate
 * provisioned for usage of Vault, it will succeed and have a token that can be
 * used to interact with Vault.
 */
public class InstanceRoleVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceRoleVaultCredentialsProvider.class);

    public static final Pattern IAM_ARN_PATTERN = Pattern.compile("(arn\\:aws\\:iam\\:\\:)(?<accountId>[0-9].*)(\\:.*)");

    private static final Pattern INSTANCE_PROFILE_ARN_PATTERN = Pattern.compile("arn:aws:iam::(.*?):instance-profile/(.*)");

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     */
    public InstanceRoleVaultCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver             Resolver for resolving the Cerberus URL
     * @param xCerberusClientOverride - Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public InstanceRoleVaultCredentialsProvider(UrlResolver urlResolver, String xCerberusClientOverride) {
        super(urlResolver, xCerberusClientOverride);
    }

    /**
     * Looks up the IAM roles assigned to the instance via the EC2 metadata
     * service. For each role assigned, an attempt is made to authenticate and
     * decrypt the Vault auth response with KMS. If successful, the token
     * retrieved is cached locally for future calls to
     * {@link BaseAwsCredentialsProvider#getCredentials()}.
     */
    @Override
    protected void authenticate() {
        try {
            final String instanceProfileArn = getInstanceProfileArn();
            final Set<String> iamRoleSet = EC2MetadataUtils.getIAMSecurityCredentials().keySet();
            Region region = Regions.getCurrentRegion();

            for (String iamRole : buildIamRoleArns(instanceProfileArn, iamRoleSet)) {
                try {
                    getAndSetToken(iamRole, region);
                    return;
                } catch (VaultClientException sce) {
                    LOGGER.warn("Unable to acquire Vault token for IAM role: " + iamRole + ", instance profile was " + instanceProfileArn, sce);
                }
            }
        } catch (AmazonClientException ace) {
            LOGGER.warn("Unexpected error communicating with AWS services.", ace);
        } catch (JsonSyntaxException jse) {
            LOGGER.error("The decrypted auth response was not in the expected format!", jse);
        }

        throw new VaultClientException("Unable to acquire token with EC2 instance role.");
    }

    /**
     * Parses and returns the AWS account ID from the instance profile ARN.
     *
     * @return AWS account ID
     * @deprecated no longer used, will be removed
     */
    protected String lookupAccountId() {
        final Matcher matcher = IAM_ARN_PATTERN.matcher(getInstanceProfileArn());

        if (matcher.matches()) {
            final String accountId = matcher.group("accountId");
            if (StringUtils.isNotBlank(accountId)) {
                return accountId;
            }
        }

        throw new VaultClientException("Unable to obtain AWS account ID from instance profile ARN.");
    }

    protected String getInstanceProfileArn() {
        EC2MetadataUtils.IAMInfo iamInfo = EC2MetadataUtils.getIAMInstanceProfileInfo();

        if (iamInfo == null) {
            final String errorMessage = "No IAM Instance Profile assigned to running instance.";
            LOGGER.error(errorMessage);
            throw new VaultClientException(errorMessage);
        }
        return iamInfo.instanceProfileArn;
    }

    /**
     * Build a set of IAM Role ARNs from the information collected from the meta-data endpoint
     *
     * @param instanceProfileArn        the instance-profile ARN
     * @param securityCredentialsKeySet a set of role names
     * @return
     */
    protected static Set<String> buildIamRoleArns(String instanceProfileArn, Set<String> securityCredentialsKeySet) {

        final Set<String> result = new HashSet<>();

        final InstanceProfileInfo instanceProfileInfo = parseInstanceProfileArn(instanceProfileArn);
        final String accountId = instanceProfileInfo.accountId;
        final String path = parsePathFromInstanceProfileName(instanceProfileInfo.profileName);

        for (String roleName : securityCredentialsKeySet) {
            result.add(buildRoleArn(accountId, path, roleName));
        }

        return result;
    }

    /**
     * Parse an instance-profile ARN into parts
     */
    protected static InstanceProfileInfo parseInstanceProfileArn(String instanceProfileArn) {
        if (instanceProfileArn == null) {
            throw new VaultClientException("instanceProfileArn provided was null rather than valid arn");
        }

        InstanceProfileInfo info = new InstanceProfileInfo();
        Matcher matcher = INSTANCE_PROFILE_ARN_PATTERN.matcher(instanceProfileArn);
        boolean found = matcher.find();
        if (!found) {
            throw new VaultClientException(String.format(
                    "Failed to find account id and role / instance profile name from ARN: %s using pattern %s",
                    instanceProfileArn, INSTANCE_PROFILE_ARN_PATTERN.pattern()));
        }

        info.accountId = matcher.group(1);
        info.profileName = matcher.group(2);

        return info;
    }

    /**
     * Parse the path out of a instanceProfileName or return null for no path
     * e.g. parse "foo/bar" out of "foo/bar/name"
     */
    protected static String parsePathFromInstanceProfileName(String instanceProfileName) {
        if (StringUtils.contains(instanceProfileName, "/")) {
            return StringUtils.substringBeforeLast(instanceProfileName, "/");
        } else {
            return null;
        }
    }

    /**
     * Build a role arn from the supplied arguments
     */
    protected static String buildRoleArn(String accountId, String path, String roleName) {
        return String.format(IAM_ROLE_ARN_FORMAT, accountId, roleWithPath(path, roleName));
    }

    /**
     * If a path is supplied, prepend it to the role name
     */
    protected static String roleWithPath(String path, String role) {
        if (StringUtils.isBlank(path)) {
            return role;
        } else {
            return StringUtils.appendIfMissing(path, "/") + role;
        }
    }

    /**
     * Bean for holding Instance Profile parse results
     */
    protected static class InstanceProfileInfo {
        String accountId;
        String profileName;
    }
}
