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

import com.google.gson.annotations.SerializedName;

public class SafeDepositBoxSummary {

	@SerializedName("id")
	private String id;

	@SerializedName("name")
	private String name;

	@SerializedName("path")
	private String path;

	@SerializedName("category_id")
	private String categoryId;

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String getCategoryId() {
		return categoryId;
	}

	@Override
	public String toString() {
		return "SafeDepositBoxSummary [id=" + id + ", name=" + name + ", path=" + path + ", categoryId=" + categoryId
				+ "]";
	}
	
}
