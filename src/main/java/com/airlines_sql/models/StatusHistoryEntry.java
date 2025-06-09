package com.airlines_sql.models;

import java.time.LocalDateTime;

public class StatusHistoryEntry {
	private final String status;
	private final LocalDateTime statusTime;

	public StatusHistoryEntry(String status, LocalDateTime statusTime) {
		this.status = status;
		this.statusTime = statusTime;
	}

	public String getStatus() { return status; }
	public LocalDateTime getStatusTime() { return statusTime; }
}