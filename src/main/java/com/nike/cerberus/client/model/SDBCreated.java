package com.nike.cerberus.client.model;

import com.google.gson.annotations.SerializedName;

public class SDBCreated {

	@SerializedName("id")
	private String id;

	@SerializedName("location")
	private String location;
	
	public String getId() {
		return id;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	@Override
	public String toString() {
		return "SDBCreated [id=" + id + ", location=" + location + "]";
	}

}
