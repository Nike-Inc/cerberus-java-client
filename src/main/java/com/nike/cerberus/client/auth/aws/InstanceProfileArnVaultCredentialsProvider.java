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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.EC2MetadataUtils;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClientException;

/**
 * This Credentials provider will look up the assigned InstanceProfileArn for this machine and attempt
 * To automatically retrieve a Vault token from CMS's iam-auth endpoint that takes region, acct id, role name.
 */
public class InstanceProfileArnVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     */
    public InstanceProfileArnVaultCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     * @param xCerberusClientOverride - Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public InstanceProfileArnVaultCredentialsProvider(UrlResolver urlResolver, String xCerberusClientOverride) {
        super(urlResolver, xCerberusClientOverride);
    }

    @Override
    protected void authenticate() {
        EC2MetadataUtils.IAMInfo iamInfo = getIamInfo();
        String instanceProfileArn = iamInfo.instanceProfileArn;
        Region region = Regions.getCurrentRegion();

        try {
            getAndSetToken(instanceProfileArn, region);
        } catch (Exception e) {
            throw new VaultClientException(String.format("Failed to authenticate with Cerberus's iam auth endpoint " +
                            "using the following auth info, iamPrincipalArn: %s, region: %s",
                    instanceProfileArn, region), e);
        }
    }

    protected EC2MetadataUtils.IAMInfo getIamInfo() {
        return EC2MetadataUtils.getIAMInstanceProfileInfo();
    }
}
