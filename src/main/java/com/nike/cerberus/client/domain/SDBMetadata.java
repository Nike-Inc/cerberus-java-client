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
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class SDBMetadata {

	@SerializedName("id")
	private String id;
	
	@SerializedName("name")
	private String name;
	
	@SerializedName("path")
	private String path;
	
	@SerializedName("category")
	private String category;
	
	@SerializedName("owner")
	private String owner;
	
	@SerializedName("description")
	private String description;
	
	@SerializedName("created_ts")
	private OffsetDateTime createdTs;
	
	@SerializedName("created_by")
	private String createdBy;
	
	@SerializedName("last_updated_ts")
	private OffsetDateTime lastUpdatedTs;
	
	@SerializedName("last_updated_by")
	private String lastUpdatedBy;
	
	@SerializedName("user_group_permissions")
	private Map<String, String> userGroupPermissions;
	
	@SerializedName("iam_role_permissions")
	private Map<String, String> iamRolePermissions;
	
	@SerializedName("data")
	private Map<String, Map<String, Object>> data;

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String getCategory() {
		return category;
	}

	public String getOwner() {
		return owner;
	}

	public String getDescription() {
		return description;
	}

	public OffsetDateTime getCreatedTs() {
		return createdTs;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public OffsetDateTime getLastUpdatedTs() {
		return lastUpdatedTs;
	}

	public String getLastUpdatedBy() {
		return lastUpdatedBy;
	}

	public Map<String, String> getUserGroupPermissions() {
		return userGroupPermissions;
	}

	public Map<String, String> getIamRolePermissions() {
		return iamRolePermissions;
	}

	public Map<String, Map<String, Object>> getData() {
		return data;
	}

	@Override
	public String toString() {
		return "SDBMetadata [id=" + id + ", name=" + name + ", path=" + path + ", category=" + category + ", owner="
				+ owner + ", description=" + description + ", createdTs=" + createdTs + ", createdBy=" + createdBy
				+ ", lastUpdatedTs=" + lastUpdatedTs + ", lastUpdatedBy=" + lastUpdatedBy + ", userGroupPermissions="
				+ userGroupPermissions + ", iamRolePermissions=" + iamRolePermissions + ", data=" + data + "]";
	}

}
