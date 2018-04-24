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

import com.fieldju.commons.EnvUtils;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.CerberusServerException;
import com.nike.cerberus.client.DefaultCerberusUrlResolver;
import com.nike.cerberus.client.model.CerberusListResponse;
import com.nike.cerberus.client.model.CerberusResponse;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests StaticIamRoleCerberusCredentialsProvider class
 */
public class CerberusClientTest {

    private static final String ROOT_SDB_PATH = "app/cerberus-integration-tests-sdb/";

    private static String iam_principal_arn;
    private static String region;

    private static String secretPath;
    private static String sdbFullSecretPath;
    private static Map<String, String> secretData;

    private static CerberusClient cerberusClient;

    private static StaticIamRoleCerberusCredentialsProvider staticIamRoleCerberusCredentialsProvider;

    @BeforeClass
    public static void setUp() {
        iam_principal_arn = EnvUtils.getRequiredEnv("TEST_IAM_PRINCIPAL_ARN", "The role to be assume by the integration test");
        region = EnvUtils.getRequiredEnv("TEST_REGION");

        EnvUtils.getRequiredEnv("CERBERUS_ADDR");

        secretPath = UUID.randomUUID().toString();
        sdbFullSecretPath = ROOT_SDB_PATH + secretPath;

        String key = RandomStringUtils.randomAlphabetic(15);
        String value = RandomStringUtils.randomAlphabetic(25);
        secretData = new HashMap<>();
        secretData.put(key, value);
    }

    private Map<String, String> generateNewSecretData() {
        String key = RandomStringUtils.randomAlphabetic(20);
        String value = RandomStringUtils.randomAlphabetic(30);
        Map<String, String> newSecretData = new HashMap<>();
        newSecretData.put(key, value);

        return newSecretData;
    }

    @Test
    public void test_cerberus_client_crud_after_auth_with_account_id_and_role_name() {
        Pattern arn_pattern = Pattern.compile("arn:aws:iam::(?<accountId>[0-9].*):role\\/(?<roleName>.*)");
        Matcher matcher = arn_pattern.matcher(iam_principal_arn);
        if (! matcher.matches()) {
            throw new AssertionError("IAM Principal ARN does not match expected format");
        }
        String account_id = matcher.group("accountId");
        String role_name = matcher.group("roleName");

        staticIamRoleCerberusCredentialsProvider = new StaticIamRoleCerberusCredentialsProvider(
                new DefaultCerberusUrlResolver(),
                account_id,
                role_name,
                region);

        cerberusClient = new CerberusClient(new DefaultCerberusUrlResolver(),
                staticIamRoleCerberusCredentialsProvider, new OkHttpClient());

        // create secret
        cerberusClient.write(sdbFullSecretPath, secretData);

        // read secret
        CerberusResponse cerberusReadResponse = cerberusClient.read(sdbFullSecretPath);
        assertEquals(secretData, cerberusReadResponse.getData());

        // list secrets
        CerberusListResponse cerberusListResponse = cerberusClient.list(ROOT_SDB_PATH);
        assertTrue(cerberusListResponse.getKeys().contains(secretPath));

        // update secret
        Map<String, String> newSecretData = generateNewSecretData();
        cerberusClient.write(sdbFullSecretPath, newSecretData);
        secretData = newSecretData;

        // confirm updated secret data
        CerberusResponse cerberusReadResponseUpdated = cerberusClient.read(sdbFullSecretPath);
        assertEquals(newSecretData, cerberusReadResponseUpdated.getData());

        // delete secret
        cerberusClient.delete(sdbFullSecretPath);

        // confirm secret is deleted
        try {
            cerberusClient.read(sdbFullSecretPath);
        } catch (CerberusServerException cse) {
            assertEquals(404, cse.getCode());
        }
    }

    @Test
    public void test_secret_is_deleted_after_auth_with_iam_principal_name() {

        staticIamRoleCerberusCredentialsProvider = new StaticIamRoleCerberusCredentialsProvider(
                new DefaultCerberusUrlResolver(),
                iam_principal_arn,
                region);

        cerberusClient = new CerberusClient(new DefaultCerberusUrlResolver(),
                staticIamRoleCerberusCredentialsProvider, new OkHttpClient());

        // create secret
        cerberusClient.write(sdbFullSecretPath, secretData);

        // read secret
        CerberusResponse cerberusReadResponse = cerberusClient.read(sdbFullSecretPath);
        assertEquals(secretData, cerberusReadResponse.getData());

        // list secrets
        CerberusListResponse cerberusListResponse = cerberusClient.list(ROOT_SDB_PATH);
        assertTrue(cerberusListResponse.getKeys().contains(secretPath));

        // update secret
        Map<String, String> newSecretData = generateNewSecretData();
        cerberusClient.write(sdbFullSecretPath, newSecretData);
        secretData = newSecretData;

        // confirm updated secret data
        CerberusResponse cerberusReadResponseUpdated = cerberusClient.read(sdbFullSecretPath);
        assertEquals(newSecretData, cerberusReadResponseUpdated.getData());

        // delete secret
        cerberusClient.delete(sdbFullSecretPath);

        // confirm secret is deleted
        try {
            cerberusClient.read(sdbFullSecretPath);
        } catch (CerberusServerException cse) {
            assertEquals(404, cse.getCode());
        }
    }

}
