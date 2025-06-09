package com.airlines_sql.models;

import java.time.LocalDateTime;

public class LocationHistoryEntry {
	private final String airportName;
	private final String iataCode;
	private final LocalDateTime recordedAt;

	public LocationHistoryEntry(String airportName, String iataCode, LocalDateTime recordedAt) {
		this.airportName = airportName;
		this.iataCode = iataCode;
		this.recordedAt = recordedAt;
	}

	public String getAirportName() { return airportName; }
	public String getIataCode() { return iataCode; }
	public LocalDateTime getRecordedAt() { return recordedAt; }
}