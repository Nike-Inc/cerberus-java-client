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

package com.nike.cerberus.client.domain;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

public class SafeDepositBoxV1 implements SafeDepositBox {

	@SerializedName("id")
	private String id;
	
	@SerializedName("category_id")
	private String categoryId;
	
	@SerializedName("name")
	private String name;
	
	@SerializedName("description")
	private String description;
	
	@SerializedName("path")
	private String path;

	@SerializedName("created_ts")
	private OffsetDateTime createdTs;

	@SerializedName("last_updated_ts")
	private OffsetDateTime lastUpdatedTs;

	@SerializedName("created_by")
	private String createdBy;

	@SerializedName("last_updated_by")
	private String lastUpdatedBy;

	@SerializedName("owner")
	private String owner;

	@SerializedName("user_group_permissions")
	private Set<UserGroupPermission> userGroupPermissions = new HashSet<>();

	@SerializedName("iam_role_permissions")
	private Set<IamRolePermission> iamRolePermissions = new HashSet<>();

	public String getId() {
		return id;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public OffsetDateTime getCreatedTs() {
		return createdTs;
	}

	public void setCreatedTs(OffsetDateTime createdTs) {
		this.createdTs = createdTs;
	}

	public OffsetDateTime getLastUpdatedTs() {
		return lastUpdatedTs;
	}

	public void setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
		this.lastUpdatedTs = lastUpdatedTs;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getLastUpdatedBy() {
		return lastUpdatedBy;
	}

	public void setLastUpdatedBy(String lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Set<UserGroupPermission> getUserGroupPermissions() {
		return userGroupPermissions;
	}

	public void setUserGroupPermissions(Set<UserGroupPermission> userGroupPermissions) {
		this.userGroupPermissions = userGroupPermissions;
	}
	
	public void addUserGroupPermission(UserGroupPermission userGroupPermission) {
		this.userGroupPermissions.add(userGroupPermission);
	}

	public Set<IamRolePermission> getIamRolePermissions() {
		return iamRolePermissions;
	}

	public void setIamRolePermissions(Set<IamRolePermission> iamRolePermissions) {
		this.iamRolePermissions = iamRolePermissions;
	}
	
	public void addIamRolePermission(IamRolePermission iamRolePermission) {
		this.iamRolePermissions.add(iamRolePermission);
	}

	@Override
	public String toString() {
		return "SafeDepositBoxV1 [id=" + id + ", categoryId=" + categoryId + ", name=" + name + ", description="
				+ description + ", path=" + path + ", createdTs=" + createdTs + ", lastUpdatedTs=" + lastUpdatedTs
				+ ", createdBy=" + createdBy + ", lastUpdatedBy=" + lastUpdatedBy + ", owner=" + owner
				+ ", userGroupPermissions=" + userGroupPermissions + ", iamRolePermissions=" + iamRolePermissions + "]";
	}

}
