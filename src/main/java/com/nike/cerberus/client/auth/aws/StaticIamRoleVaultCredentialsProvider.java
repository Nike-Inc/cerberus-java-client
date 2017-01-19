package com.nike.cerberus.client.auth.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.UrlResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class StaticIamRoleVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    protected String accountId;
    protected String roleName;
    protected Region region;

    public StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver, String accountId, String roleName, String region) {
        this(urlResolver);
        this.accountId = accountId;
        this.roleName = roleName;
        this.region = Region.getRegion(Regions.fromName(region));
    }

    public StaticIamRoleVaultCredentialsProvider(String vaultUrl, String accountId, String roleName, String region) {
        this(new StaticVaultUrlResolver(vaultUrl));
        this.accountId = accountId;
        this.roleName = roleName;
        this.region = Region.getRegion(Regions.fromName(region));
    }

    public StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver, String accountId, String roleName, Region region) {
        this(urlResolver);
        this.accountId = accountId;
        this.roleName = roleName;
        this.region = region;
    }

    public StaticIamRoleVaultCredentialsProvider(String vaultUrl, String accountId, String roleName, Region region) {
        this(new StaticVaultUrlResolver(vaultUrl));
        this.accountId = accountId;
        this.roleName = roleName;
        this.region = region;
    }

    public StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver, String iamRoleArn, String region) {
        this(urlResolver);
        this.accountId = getAccountIdFromArn(iamRoleArn);
        this.roleName = getRoleNameFromArn(iamRoleArn);
        this.region = Region.getRegion(Regions.fromName(region));
    }


    public StaticIamRoleVaultCredentialsProvider(String vaultUrl, String iamRoleArn, String region) {
        this(new StaticVaultUrlResolver(vaultUrl));
        this.accountId = getAccountIdFromArn(iamRoleArn);
        this.roleName = getRoleNameFromArn(iamRoleArn);
        this.region = Region.getRegion(Regions.fromName(region));
    }


    public StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver, String iamRoleArn, Region region) {
        this(urlResolver);
        this.accountId = getAccountIdFromArn(iamRoleArn);
        this.roleName = getRoleNameFromArn(iamRoleArn);
        this.region = region;
    }

    public StaticIamRoleVaultCredentialsProvider(String vaultUrl, String iamRoleArn, Region region) {
        this(new StaticVaultUrlResolver(vaultUrl));
        this.accountId = getAccountIdFromArn(iamRoleArn);
        this.roleName = getRoleNameFromArn(iamRoleArn);
        this.region = region;
    }

    private StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
    }

    private String getAccountIdFromArn(String arn) {
        Matcher m = Pattern.compile("arn:aws:iam::(.*?):role.*").matcher(arn);
        boolean found = m.find();
        if (found) {
            return m.group(1);
        }

        throw new IllegalArgumentException("Invalid IAM role ARN supplied, expected arn:aws:iam::%s:role/%s");
    }

    private String getRoleNameFromArn(String arn) {
        Matcher m = Pattern.compile("arn:aws:iam::.*?:role/(.*)").matcher(arn);
        boolean found = m.find();
        if (found) {
            return m.group(1);
        }

        throw new IllegalArgumentException("Invalid IAM role ARN supplied, expected arn:aws:iam::%s:role/%s");
    }

    @Override
    protected void authenticate() {
        getAndSetToken(accountId, roleName, region);
    }
}
