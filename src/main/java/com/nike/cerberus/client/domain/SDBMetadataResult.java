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

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class SDBMetadataResult {

	@SerializedName("has_next")
	private boolean hasNext = false;
	
	@SerializedName("next_offset")
	private int nextOffset = 0;
	
	@SerializedName("limit")
	private int limit = 0;
	
	@SerializedName("offset")
	private int offset = 0;
	
	@SerializedName("sdb_count_in_result")
	private int sdbCountInResult;
	
	@SerializedName("total_sdbcount")
	private int totalSDBCount;
	
	@SerializedName("safe_deposit_box_metadata")
	private List<SDBMetadata> safeDepositBoxMetadata;

	public boolean isHasNext() {
		return hasNext;
	}

	public int getNextOffset() {
		return nextOffset;
	}

	public int getLimit() {
		return limit;
	}

	public int getOffset() {
		return offset;
	}

	public int getSdbCountInResult() {
		return sdbCountInResult;
	}

	public int getTotalSDBCount() {
		return totalSDBCount;
	}

	public List<SDBMetadata> getSafeDepositBoxMetadata() {
		return safeDepositBoxMetadata;
	}

	@Override
	public String toString() {
		return "SDBMetadataResult [hasNext=" + hasNext + ", nextOffset=" + nextOffset + ", limit=" + limit + ", offset="
				+ offset + ", sdbCountInResult=" + sdbCountInResult + ", totalSDBCount=" + totalSDBCount
				+ ", safeDepositBoxMetadata=" + safeDepositBoxMetadata + "]";
	}

}
