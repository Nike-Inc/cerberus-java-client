package com.nike.cerberus.client.model;

public class SDBCreated {

	private String id;
	private String location;
	
	public SDBCreated() {}
	public SDBCreated(String id, String location) {
		this.id = id;
		this.location = location;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
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
