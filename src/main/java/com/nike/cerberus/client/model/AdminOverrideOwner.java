package com.nike.cerberus.client.model;

import com.google.gson.annotations.SerializedName;

public class AdminOverrideOwner {

	@SerializedName("name")
	private String name;
	
	@SerializedName("owner")
	private String owner;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public String toString() {
		return "AdminOverrideOwner [name=" + name + ", owner=" + owner + "]";
	}
	
}
