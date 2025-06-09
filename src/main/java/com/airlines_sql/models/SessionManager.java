package com.airlines_sql.models;

public class SessionManager {
	private static final SessionManager INSTANCE = new SessionManager();

	private Integer currentCustomerId;
	private String  currentUserRole;   // “USER” \ “ADMIN”

	private SessionManager() { }

	public static SessionManager getInstance() {
		return INSTANCE;
	}

	public void setCurrentCustomerId(Integer id) {
		this.currentCustomerId = id;
	}

	public Integer getCurrentCustomerId() {
		return currentCustomerId;
	}

	public void setCurrentUserRole(String role) {
		this.currentUserRole = role;
	}

	public String getCurrentUserRole() {
		return currentUserRole;
	}

	public void clearSession() {
		this.currentCustomerId = null;
		this.currentUserRole   = null;
	}
}
