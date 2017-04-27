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
public class InstanceProfileVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     */
    public InstanceProfileVaultCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
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
