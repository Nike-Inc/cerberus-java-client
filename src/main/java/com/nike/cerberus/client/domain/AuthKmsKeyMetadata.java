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

public class AuthKmsKeyMetadata {

	@SerializedName("aws_iam_role_arn")
	private String awsIamRoleArn;
	
	@SerializedName("aws_kms_key_id")
	private String awsKmsKeyId;
	
	@SerializedName("aws_region")
	private String awsRegion;
	
	@SerializedName("created_ts")
	private OffsetDateTime createdTs;
	
	@SerializedName("last_updated_ts")
	private OffsetDateTime lastUpdatedTs;
	
	@SerializedName("last_validated_ts")
	private OffsetDateTime lastValidatedTs;

	public String getAwsIamRoleArn() {
		return awsIamRoleArn;
	}

	public String getAwsKmsKeyId() {
		return awsKmsKeyId;
	}

	public String getAwsRegion() {
		return awsRegion;
	}

	public OffsetDateTime getCreatedTs() {
		return createdTs;
	}

	public OffsetDateTime getLastUpdatedTs() {
		return lastUpdatedTs;
	}

	public OffsetDateTime getLastValidatedTs() {
		return lastValidatedTs;
	}

	@Override
	public String toString() {
		return "AuthKmsKeyMetadata [awsIamRoleArn=" + awsIamRoleArn + ", awsKmsKeyId=" + awsKmsKeyId + ", awsRegion="
				+ awsRegion + ", createdTs=" + createdTs + ", lastUpdatedTs=" + lastUpdatedTs + ", lastValidatedTs="
				+ lastValidatedTs + "]";
	}

}
