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

import java.util.List;

/** Represents a specific safe deposit box. */
public class CerberusSafeDepositBoxResponse {

    private String id;
    private String name;
    private String path;
    private String categoryId;
    private String owner;
    private String description;
    private List<CerberusUserGroupPermission> userGroupPermissions;
    private List<CerberusIamPrincipalPermission> iamPrincipalPermissions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CerberusUserGroupPermission> getUserGroupPermissions() {
        return userGroupPermissions;
    }

    public void setUserGroupPermissions(List<CerberusUserGroupPermission> userGroupPermissions) {
        this.userGroupPermissions = userGroupPermissions;
    }

    public List<CerberusIamPrincipalPermission> getIamPrincipalPermissions() {
        return iamPrincipalPermissions;
    }

    public void setIamPrincipalPermissions(List<CerberusIamPrincipalPermission> iamPrincipalPermissions) {
        this.iamPrincipalPermissions = iamPrincipalPermissions;
    }
}
