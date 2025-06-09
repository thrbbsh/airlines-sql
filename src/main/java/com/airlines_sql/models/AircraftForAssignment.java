package com.airlines_sql.models;

public class AircraftForAssignment {
	private final int aircraftId;
	private final String modelName;
	private final int range;

	public AircraftForAssignment(int aircraftId, String modelName, int range) {
		this.aircraftId = aircraftId;
		this.modelName = modelName;
		this.range = range;
	}

	public int getAircraftId() { return aircraftId; }

	@Override
	public String toString() {
		return String.format("ID: %d | %s (Range: %d km)", aircraftId, modelName, range);
	}
}