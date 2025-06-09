package com.airlines_sql.models;

import java.time.LocalDateTime;

public class FlightForIncident {
	private final int instanceId;
	private final int aircraftId;
	private final String flightStatus;
	private final LocalDateTime departure;
	private final String route; // Например, "JFK -> LAX"

	public FlightForIncident(int instanceId, int aircraftId, String flightStatus, LocalDateTime departure, String route) {
		this.instanceId = instanceId;
		this.aircraftId = aircraftId;
		this.flightStatus = flightStatus;
		this.departure = departure;
		this.route = route;
	}

	public int getInstanceId() { return instanceId; }
	public String getFlightStatus() { return flightStatus; }
	public int getAircraftId() { return aircraftId; }
	public String getRoute() { return route;}

	@Override
	public String toString() {
		return String.format("Flight ID: %d | Aircraft: %d | Route: %s | Status: %s",
				instanceId, aircraftId, route, flightStatus);
	}
}