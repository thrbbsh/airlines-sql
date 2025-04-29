CREATE TABLE aircraft (
    aircraft_id SERIAL PRIMARY KEY,
    model_id INT NOT NULL REFERENCES aircraft_model(model_id),
    manufacture_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE aircraft_model (
    model_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    passenger_count INT NOT NULL,
    range_km INT NOT NULL,
    fuel_consumption DECIMAL(5,2) NOT NULL
);

CREATE TABLE airport (
    airport_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    country VARCHAR(50) NOT NULL
);

CREATE TABLE route (
    route_id SERIAL PRIMARY KEY,
    airport_from INT NOT NULL REFERENCES airport(airport_id),
    airport_to INT NOT NULL REFERENCES airport(airport_id),
    distance_km INT NOT NULL
);

CREATE TABLE flight (
    flight_id SERIAL PRIMARY KEY,
    aircraft_id INT NOT NULL REFERENCES aircraft(aircraft_id),
    route_id INT NOT NULL REFERENCES route(route_id),
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE customer (
    customer_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20)
);

CREATE TABLE booking (
    booking_id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES customer(customer_id),
    booking_date TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL
);

CREATE TABLE fare (
    fare_id SERIAL PRIMARY KEY,
    class VARCHAR(20) NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    baggage_allowance_kg INT NOT NULL,
    rules TEXT
);

CREATE TABLE ticket (
    ticket_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES booking(booking_id),
    route_id INT NOT NULL REFERENCES route(route_id),
    seat_number VARCHAR(5) NOT NULL,
    fare_id INT NOT NULL REFERENCES fare(fare_id),
    price DECIMAL(10,2) NOT NULL,
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE payment (
    payment_id SERIAL PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES ticket(ticket_id),
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    payment_datetime TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE crew (
    crew_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(30) NOT NULL,
    license_number VARCHAR(50),
    hire_date DATE NOT NULL
);

CREATE TABLE flight_crew_assignment (
    assignment_id SERIAL PRIMARY KEY,
    flight_id INT NOT NULL REFERENCES flight(flight_id),
    crew_id INT NOT NULL REFERENCES crew(crew_id)
);

CREATE TABLE seat_layout (
    aircraft_id INT NOT NULL REFERENCES aircraft(aircraft_id),
    seat_id VARCHAR(5) NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (aircraft_id, seat_id)
);

CREATE TABLE maintenance (
    maintenance_id SERIAL PRIMARY KEY,
    aircraft_id INT NOT NULL REFERENCES aircraft(aircraft_id),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    description TEXT,
    cost DECIMAL(12,2)
);

CREATE TABLE baggage (
    baggage_id SERIAL PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES ticket(ticket_id),
    weight_kg DECIMAL(5,2) NOT NULL,
    dimensions VARCHAR(20),
    status VARCHAR(30) NOT NULL
);

CREATE TABLE incident (
    incident_id SERIAL PRIMARY KEY,
    aircraft_id INT NOT NULL REFERENCES aircraft(aircraft_id),
    type VARCHAR(30) NOT NULL,
    description TEXT,
    delay_minutes INT
);

CREATE TABLE cargo_flight (
    cargo_id SERIAL PRIMARY KEY,
    aircraft_id INT NOT NULL REFERENCES aircraft(aircraft_id),
    flight_id INT NOT NULL REFERENCES flight(flight_id),
    description TEXT,
    weight_kg DECIMAL(8,2),
    dimensions VARCHAR(20)
);

CREATE TABLE promo_code (
    promo_id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    discount_pct DECIMAL(5,2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL
);

CREATE TABLE review (
    review_id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES customer(customer_id),
    flight_id INT NOT NULL REFERENCES flight(flight_id),
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 10),
    comment TEXT,
    review_date DATE NOT NULL DEFAULT CURRENT_DATE
);
