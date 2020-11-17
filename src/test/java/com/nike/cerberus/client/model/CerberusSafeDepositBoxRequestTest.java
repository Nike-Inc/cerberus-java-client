/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.client.model;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class CerberusSafeDepositBoxRequestTest {
    @Test
    public void test_withUserGroupPermission_overrides() {
        CerberusSafeDepositBoxRequest request = CerberusSafeDepositBoxRequest.newBuilder()
                .withName("foo")
                .withOwner("bar")
                .withUserGroupPermission("user group 1", "role id 1")
                .withUserGroupPermission("user group 1", "role id 2")
                .build();

        assertEquals("user group 1", request.getUserGroupPermissions().get(0).getName());
        assertEquals("role id 2", request.getUserGroupPermissions().get(0).getRoleId());
    }

    @Test
    public void test_withIamPrincipalPermission_overrides() {
        CerberusSafeDepositBoxRequest request = CerberusSafeDepositBoxRequest.newBuilder()
                .withName("foo")
                .withOwner("bar")
                .withIamPrincipalPermission("iam principal 1", "role id 1")
                .withIamPrincipalPermission("iam principal 1", "role id 2")
                .build();

        assertEquals("iam principal 1", request.getIamPrincipalPermissions().get(0).getIamPrincipalArn());
        assertEquals("role id 2", request.getIamPrincipalPermissions().get(0).getRoleId());
    }

    @Test
    public void test_withRolePermissionMap() {
        CerberusSafeDepositBoxRequest request = CerberusSafeDepositBoxRequest.newBuilder()
                .withName("foo")
                .withOwner("bar")
                .withRolePermissionMap(Collections.singletonMap(CerberusRolePermission.OWNER, "role id 1"))
                .withUserGroupPermission("user group 1", CerberusRolePermission.OWNER)
                .build();

        assertEquals("user group 1", request.getUserGroupPermissions().get(0).getName());
        assertEquals("role id 1", request.getUserGroupPermissions().get(0).getRoleId());
    }
}