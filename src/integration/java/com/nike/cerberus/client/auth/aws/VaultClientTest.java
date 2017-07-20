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

import com.fieldju.commons.EnvUtils;
import com.nike.cerberus.client.DefaultCerberusUrlResolver;
import com.nike.vault.client.VaultClient;
import com.nike.vault.client.VaultServerException;
import com.nike.vault.client.model.VaultListResponse;
import com.nike.vault.client.model.VaultResponse;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests StaticIamRoleVaultCredentialsProvider class
 */
public class VaultClientTest {

    private static final String ROOT_SDB_PATH = "app/cerberus-integration-tests-sdb/";

    private static String account_id;
    private static String role_name;
    private static String iam_principal_arn;
    private static String region;

    private static String secretPath;
    private static String sdbFullSecretPath;
    private static Map<String, String> secretData;

    private static VaultClient vaultClient;

    private static StaticIamRoleVaultCredentialsProvider staticIamRoleVaultCredentialsProvider;

    @BeforeClass
    public static void setUp() {
        account_id = EnvUtils.getRequiredEnv("TEST_ACCOUNT_ID", "TEST_ACCOUNT_ID is used to assume a role in the given account");
        role_name = EnvUtils.getRequiredEnv("TEST_ROLE_NAME", "role_name is role to assume to auth with Cerberus");
        region = EnvUtils.getRequiredEnv("TEST_REGION");

        EnvUtils.getRequiredEnv("CERBERUS_ADDR");

        iam_principal_arn = String.format("arn:aws:iam::%s:role/%s", account_id, role_name);
        secretPath = UUID.randomUUID().toString();
        sdbFullSecretPath = ROOT_SDB_PATH + secretPath;

        String key = RandomStringUtils.randomAlphabetic(15);
        String value = RandomStringUtils.randomAlphabetic(25);
        secretData = new HashMap<>();
        secretData.put(key, value);
    }

    @AfterClass
    public static void tearDown() {
        vaultClient.delete(sdbFullSecretPath);
    }

    private Map<String, String> generateNewSecretData() {
        String key = RandomStringUtils.randomAlphabetic(20);
        String value = RandomStringUtils.randomAlphabetic(30);
        Map<String, String> newSecretData = new HashMap<>();
        newSecretData.put(key, value);

        return newSecretData;
    }

    @Test
    public void test_vault_client_crud_after_auth_with_account_id_and_role_name() {

        staticIamRoleVaultCredentialsProvider = new StaticIamRoleVaultCredentialsProvider(
                new DefaultCerberusUrlResolver(),
                account_id,
                role_name,
                region);

        vaultClient = new VaultClient(new DefaultCerberusUrlResolver(),
                staticIamRoleVaultCredentialsProvider, new OkHttpClient());

        // create secret
        vaultClient.write(sdbFullSecretPath, secretData);

        // read secret
        VaultResponse vaultReadResponse = vaultClient.read(sdbFullSecretPath);
        assertEquals(secretData, vaultReadResponse.getData());

        // list secrets
        VaultListResponse vaultListResponse = vaultClient.list(ROOT_SDB_PATH);
        assertTrue(vaultListResponse.getKeys().contains(secretPath));

        // update secret
        Map<String, String> newSecretData = generateNewSecretData();
        vaultClient.write(sdbFullSecretPath, newSecretData);
        secretData = newSecretData;

        // confirm updated secret data
        VaultResponse vaultReadResponseUpdated = vaultClient.read(sdbFullSecretPath);
        assertEquals(newSecretData, vaultReadResponseUpdated.getData());

        // delete secret
        vaultClient.delete(sdbFullSecretPath);

        // confirm secret is deleted
        try {
            vaultClient.read(sdbFullSecretPath);
        } catch (VaultServerException vse) {
            assertEquals(404, vse.getCode());
        }
    }

    @Test
    public void test_secret_is_deleted_after_auth_with_iam_principal_name() {

        staticIamRoleVaultCredentialsProvider = new StaticIamRoleVaultCredentialsProvider(
                new DefaultCerberusUrlResolver(),
                iam_principal_arn,
                region);

        vaultClient = new VaultClient(new DefaultCerberusUrlResolver(),
                staticIamRoleVaultCredentialsProvider, new OkHttpClient());

        // create secret
        vaultClient.write(sdbFullSecretPath, secretData);

        // read secret
        VaultResponse vaultReadResponse = vaultClient.read(sdbFullSecretPath);
        assertEquals(secretData, vaultReadResponse.getData());

        // list secrets
        VaultListResponse vaultListResponse = vaultClient.list(ROOT_SDB_PATH);
        assertTrue(vaultListResponse.getKeys().contains(secretPath));

        // update secret
        Map<String, String> newSecretData = generateNewSecretData();
        vaultClient.write(sdbFullSecretPath, newSecretData);
        secretData = newSecretData;

        // confirm updated secret data
        VaultResponse vaultReadResponseUpdated = vaultClient.read(sdbFullSecretPath);
        assertEquals(newSecretData, vaultReadResponseUpdated.getData());

        // delete secret
        vaultClient.delete(sdbFullSecretPath);

        // confirm secret is deleted
        try {
            vaultClient.read(sdbFullSecretPath);
        } catch (VaultServerException vse) {
            assertEquals(404, vse.getCode());
        }
    }

}
