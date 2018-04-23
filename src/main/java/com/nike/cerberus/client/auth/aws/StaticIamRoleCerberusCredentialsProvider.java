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
import com.nike.cerberus.client.StaticCerberusUrlResolver;
import com.nike.cerberus.client.UrlResolver;

/**
 * Provider for allowing users to explicitly set the account id, role name and region that they want to authenticate as.
 */
public class StaticIamRoleCerberusCredentialsProvider extends BaseAwsCredentialsProvider {

    public static final String IAM_ROLE_ARN_FORMAT = "arn:aws:iam::%s:role/%s";
    protected String iamPrincipalArn;
    protected Region region;

    public StaticIamRoleCerberusCredentialsProvider(UrlResolver urlResolver, String accountId, String roleName, String region) {
        this(urlResolver);
        this.iamPrincipalArn = generateIamRoleArn(accountId, roleName);
        this.region = Region.getRegion(Regions.fromName(region));
    }

    public StaticIamRoleCerberusCredentialsProvider(String cerberusUrl, String accountId, String roleName, String region) {
        this(new StaticCerberusUrlResolver(cerberusUrl));
        this.iamPrincipalArn = generateIamRoleArn(accountId, roleName);
        this.region = Region.getRegion(Regions.fromName(region));
    }

    public StaticIamRoleCerberusCredentialsProvider(UrlResolver urlResolver, String accountId, String roleName, Region region) {
        this(urlResolver);
        this.iamPrincipalArn = generateIamRoleArn(accountId, roleName);
        this.region = region;
    }

    public StaticIamRoleCerberusCredentialsProvider(String cerberusUrl, String accountId, String roleName, Region region) {
        this(new StaticCerberusUrlResolver(cerberusUrl));
        this.iamPrincipalArn = generateIamRoleArn(accountId, roleName);
        this.region = region;
    }

    public StaticIamRoleCerberusCredentialsProvider(UrlResolver urlResolver, String iamRoleArn, String region) {
        this(urlResolver);
        this.iamPrincipalArn = iamRoleArn;
        this.region = Region.getRegion(Regions.fromName(region));
    }


    public StaticIamRoleCerberusCredentialsProvider(String cerberusUrl, String iamRoleArn, String region) {
        this(new StaticCerberusUrlResolver(cerberusUrl));
        this.iamPrincipalArn = iamRoleArn;
        this.region = Region.getRegion(Regions.fromName(region));
    }


    public StaticIamRoleCerberusCredentialsProvider(UrlResolver urlResolver, String iamRoleArn, Region region) {
        this(urlResolver);
        this.iamPrincipalArn = iamRoleArn;
        this.region = region;
    }

    public StaticIamRoleCerberusCredentialsProvider(String cerberusUrl, String iamRoleArn, Region region) {
        this(new StaticCerberusUrlResolver(cerberusUrl));
        this.iamPrincipalArn = iamRoleArn;
        this.region = region;
    }

    private StaticIamRoleCerberusCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
    }

    private String generateIamRoleArn(String accountId, String roleName) {

        return String.format(IAM_ROLE_ARN_FORMAT, accountId, roleName);
    }

    @Override
    protected void authenticate() {
        getAndSetToken(iamPrincipalArn, region);
    }
}
