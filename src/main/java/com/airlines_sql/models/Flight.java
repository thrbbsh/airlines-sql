package com.airlines_sql.models;

import java.sql.Timestamp;

public class Flight {
	private final Integer flightInstanceId;
	private final String  originCity;
	private final String  originIata;
	private final String  destinationCity;
	private final String  destinationIata;
	private final Timestamp scheduledDeparture;
	private final Timestamp scheduledArrival;
	private final Double  price;
	private final boolean available;

	public Flight(Integer instId,
				  String originCity, String originIata,
				  String destinationCity, String destinationIata,
				  Timestamp dep, Timestamp arr,
				  Double price) {
		this.flightInstanceId   = instId;
		this.originCity         = originCity;
		this.originIata         = originIata;
		this.destinationCity    = destinationCity;
		this.destinationIata    = destinationIata;
		this.scheduledDeparture = dep;
		this.scheduledArrival   = arr;
		this.price              = price;
		this.available          = (instId != null);
	}

	public Integer getFlightInstanceId() { return flightInstanceId; }
	public String  getOriginCity()       { return originCity; }
	public String  getOriginIata()       { return originIata; }
	public String  getDestinationCity()  { return destinationCity; }
	public String  getDestinationIata()  { return destinationIata; }
	public Timestamp getScheduledDeparture() { return scheduledDeparture; }
	public Timestamp getScheduledArrival()   { return scheduledArrival; }
	public Double  getPrice()            { return available ? price : null; }
	public String  getStatus()           { return available ? "Available" : "Not issued"; }
	public boolean isAvailable()         { return available; }
}
