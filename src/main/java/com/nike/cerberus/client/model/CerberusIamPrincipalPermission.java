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

/** Represents an IAM principal permission object for a specific safe deposit box. */
public class CerberusIamPrincipalPermission {
    private String iamPrincipalArn;
    private String roleId;

    public String getIamPrincipalArn() {
        return iamPrincipalArn;
    }

    public void setIamPrincipalArn(String iamPrincipalArn) {
        this.iamPrincipalArn = iamPrincipalArn;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
}
