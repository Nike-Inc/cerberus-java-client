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

public class SecureDataVersionSummary {

	@SerializedName("id")
	private String id;

	@SerializedName("sdb_id")
	private String sdboxId;

	@SerializedName("path")
	private String path;

	@SerializedName("action")
	private String action;

	@SerializedName("type")
	private SecureDataType type;

	@SerializedName("size_in_bytes")
	private int sizeInBytes;

	@SerializedName("version_created_by")
	private String versionCreatedBy;

	@SerializedName("version_created_ts")
	private OffsetDateTime versionCreatedTs;

	@SerializedName("action_principal")
	private String actionPrincipal;

	@SerializedName("action_ts")
	private OffsetDateTime actionTs;

	public String getId() {
		return id;
	}

	public String getSdboxId() {
		return sdboxId;
	}

	public String getPath() {
		return path;
	}

	public String getAction() {
		return action;
	}

	public SecureDataType getType() {
		return type;
	}

	public int getSizeInBytes() {
		return sizeInBytes;
	}

	public String getVersionCreatedBy() {
		return versionCreatedBy;
	}

	public OffsetDateTime getVersionCreatedTs() {
		return versionCreatedTs;
	}

	public String getActionPrincipal() {
		return actionPrincipal;
	}

	public OffsetDateTime getActionTs() {
		return actionTs;
	}

	@Override
	public String toString() {
		return "SecureDataVersionSummary [id=" + id + ", sdboxId=" + sdboxId + ", path=" + path + ", action=" + action
				+ ", type=" + type + ", sizeInBytes=" + sizeInBytes + ", versionCreatedBy=" + versionCreatedBy
				+ ", versionCreatedTs=" + versionCreatedTs + ", actionPrincipal=" + actionPrincipal + ", actionTs="
				+ actionTs + "]";
	}

}
