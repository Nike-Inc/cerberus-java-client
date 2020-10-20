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

import com.google.gson.annotations.SerializedName;

public class UserGroupPermission {

	@SerializedName("id")
	private String id;

	@SerializedName("name")
	private String name;

	@SerializedName("role_id")
	private String roleId;

	@SerializedName("created_ts")
	private OffsetDateTime createdTs;

	@SerializedName("last_updated_ts")
	private OffsetDateTime lastUpdatedTs;

	@SerializedName("created_by")
	private String createdBy;

	@SerializedName("last_updated_by")
	private String lastUpdatedBy;

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

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
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

	@Override
	public String toString() {
		return "UserGroupPermission [id=" + id + ", name=" + name + ", roleId=" + roleId + ", createdTs=" + createdTs
				+ ", lastUpdatedTs=" + lastUpdatedTs + ", createdBy=" + createdBy + ", lastUpdatedBy=" + lastUpdatedBy
				+ "]";
	}
	
	
}
