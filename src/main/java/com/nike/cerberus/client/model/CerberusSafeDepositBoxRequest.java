package com.nike.cerberus.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CerberusSafeDepositBoxRequest {
    private String name;
    private String categoryId;
    private String description;
    private String owner;
    private List<CerberusUserGroupPermission> userGroupPermissions;
    private List<CerberusIamPrincipalPermission> iamPrincipalPermissions;

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

        public Builder withCerberusSafeDepositBoxResponse(CerberusSafeDepositBoxResponse sdbResponse) {
            this.name = sdbResponse.getName();
            this.description = sdbResponse.getDescription();
            this.owner = sdbResponse.getOwner();
            this.userGroupPermissions = sdbResponse.getUserGroupPermissions();
            this.iamPrincipalPermissions = sdbResponse.getIamPrincipalPermissions();
            return this;
        }

        public CerberusSafeDepositBoxRequest build() {
            if (name == null) {
                throw new IllegalArgumentException("name must be set");
            }

            if (owner == null) {
                throw new IllegalArgumentException("owner must be set");
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
