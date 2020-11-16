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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Represents a request for creating and updating a safe deposit box. */
public class CerberusSafeDepositBoxRequest {
    private String name;
    private String categoryId;
    private String description;
    private String owner;
    private List<CerberusUserGroupPermission> userGroupPermissions;
    private List<CerberusIamPrincipalPermission> iamPrincipalPermissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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

    public static class Builder {
        private String name;
        private String categoryId;
        private String description;
        private String owner;
        private List<CerberusUserGroupPermission> userGroupPermissions;
        private List<CerberusIamPrincipalPermission> iamPrincipalPermissions;
        private Map<CerberusRolePermission, String> rolePermissionMap;

        public Builder withName(String name){
            this.name = name;
            return this;
        }

        public Builder withCategoryId(String categoryId){
            this.categoryId = categoryId;
            return this;
        }

        public Builder withOwner(String owner){
            this.owner = owner;
            return this;
        }

        public Builder withDescription(String description){
            this.description = description;
            return this;
        }

        public Builder withRolePermissionMap(Map<CerberusRolePermission, String> rolePermissionMap){
            this.rolePermissionMap = rolePermissionMap;
            return this;
        }

        public Builder withUserGroupPermission(String name, String roleId){
            if (userGroupPermissions == null) {
                userGroupPermissions = new ArrayList<>();
            }
            Optional<CerberusUserGroupPermission> any = userGroupPermissions.stream().filter(permission -> name.equals(permission.getName())).findAny();
            if (any.isPresent()){
                any.get().setRoleId(roleId);
            } else {
                CerberusUserGroupPermission userGroupPermission = new CerberusUserGroupPermission();
                userGroupPermission.setName(name);
                userGroupPermission.setRoleId(roleId);
                userGroupPermissions.add(userGroupPermission);
            }
            return this;
        }

        public Builder removeUserGroupPermission(String userGroupNameToBeRemoved){
            if (userGroupPermissions != null){
                userGroupPermissions.removeIf(userGroupPermission -> userGroupPermission.getName().equals(userGroupNameToBeRemoved));
            }
            return this;
        }

        public Builder withIamPrincipalPermission(String iamPrincipalArn, String roleId){
            if (iamPrincipalPermissions == null) {
                iamPrincipalPermissions = new ArrayList<>();
            }
            Optional<CerberusIamPrincipalPermission> any = iamPrincipalPermissions.stream().filter(permission -> iamPrincipalArn.equals(permission.getIamPrincipalArn())).findAny();
            if (any.isPresent()){
                any.get().setRoleId(roleId);
            } else {
                CerberusIamPrincipalPermission iamPrincipalPermission = new CerberusIamPrincipalPermission();
                iamPrincipalPermission.setIamPrincipalArn(iamPrincipalArn);
                iamPrincipalPermission.setRoleId(roleId);
                iamPrincipalPermissions.add(iamPrincipalPermission);
            }
            return this;
        }

        public Builder withUserGroupPermission(String name, CerberusRolePermission rolePermission){
            if (rolePermissionMap == null) {
                throw new IllegalStateException("withRolePermissionMap() needs to be called before calling this method");
            }
            return withUserGroupPermission(name, rolePermissionMap.get(rolePermission));
        }

        public Builder withIamPrincipalPermission(String iamPrincipalArn, CerberusRolePermission rolePermission){
            if (rolePermissionMap == null) {
                throw new IllegalStateException("withRolePermissionMap() needs to be called before calling this method");
            }
            return withIamPrincipalPermission(iamPrincipalArn, rolePermissionMap.get(rolePermission));
        }

        public Builder removeIamPrincipalPermission(String iamPrincipalArnToBeRemoved){
            if (iamPrincipalPermissions != null){
                iamPrincipalPermissions.removeIf(iamPrincipalPermission -> iamPrincipalPermission.getIamPrincipalArn().equals(iamPrincipalArnToBeRemoved));
            }
            return this;
        }

        public Builder withCerberusSafeDepositBoxResponse(CerberusSafeDepositBoxResponse sdbResponse) {
            this.name = sdbResponse.getName();
            this.description = sdbResponse.getDescription();
            this.owner = sdbResponse.getOwner();
            this.categoryId = sdbResponse.getCategoryId();
            this.userGroupPermissions = sdbResponse.getUserGroupPermissions();
            this.iamPrincipalPermissions = sdbResponse.getIamPrincipalPermissions();
            return this;
        }

        public CerberusSafeDepositBoxRequest build() {
            if (name == null) {
                throw new IllegalArgumentException("Safe deposit box name must be set");
            }

            if (owner == null) {
                throw new IllegalArgumentException("Safe deposit box owner must be set");
            }

            return new CerberusSafeDepositBoxRequest(this);
        }


    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private CerberusSafeDepositBoxRequest(Builder builder) {
        this.name = builder.name;
        this.categoryId = builder.categoryId;
        this.description = builder.description;
        this.owner = builder.owner;
        this.userGroupPermissions = builder.userGroupPermissions;
        this.iamPrincipalPermissions = builder.iamPrincipalPermissions;
    }
}
