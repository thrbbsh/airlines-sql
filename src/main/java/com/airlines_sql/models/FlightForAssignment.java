package com.airlines_sql.models;

import java.time.LocalDateTime;

public class FlightForAssignment {
	private final int instanceId;
	private final LocalDateTime scheduledDeparture;
	private final String fromIata;
	private final String toIata;
	private final int distance;

	public FlightForAssignment(int instanceId, LocalDateTime scheduledDeparture, String fromIata, String toIata, int distance) {
		this.instanceId = instanceId;
		this.scheduledDeparture = scheduledDeparture;
		this.fromIata = fromIata;
		this.toIata = toIata;
		this.distance = distance;
	}

	public int getInstanceId() { return instanceId; }
	public LocalDateTime getScheduledDeparture() { return scheduledDeparture; }
	public String getFromIata() { return fromIata; }
	public String getToIata() { return toIata; }
	public int getDistance() { return distance; }
}