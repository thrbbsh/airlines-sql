CREATE TYPE weekday AS ENUM (
    'Mon',
    'Tue',
    'Wed',
    'Thu',
    'Fri',
    'Sat',
    'Sun'
);

CREATE TYPE user_role AS ENUM (
    'USER',
    'ADMIN'
);

CREATE TYPE flight_status_enum AS ENUM (
    'Scheduled',
    'Departed',
    'Delayed',
    'Cancelled',
    'Landed'
);

CREATE TYPE aircraft_status_enum AS ENUM (
    'InService',
    'UnderMaintenance',
    'Retired',
    'Storage'
);


CREATE TYPE booking_status_enum AS ENUM (
    'Pending',
    'Confirmed',
    'Cancelled',
    'Completed'
);

CREATE TYPE seat_type_enum AS ENUM (
    'Window',
    'Aisle',
    'Middle',
    'ExtraLegroom'
);

CREATE TYPE incident_type_enum AS ENUM (
    'Technical',
    'Weather',
    'Security',
    'Other'
);

CREATE TYPE fare_class_enum AS ENUM (
    'Economy',
    'Business',
    'First'
);

-- basic stuff

CREATE TABLE country (
    country_id   SERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    iso_code     CHAR(2) NOT NULL UNIQUE
);

CREATE TABLE city (
    city_id      SERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    country_id   INT NOT NULL REFERENCES country(country_id)
);

CREATE TABLE airport (
    airport_id   SERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    city_id      INT NOT NULL REFERENCES city(city_id)
);

-- aircrafts

CREATE TABLE aircraft_model (
    model_id           SERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    passenger_count    INT NOT NULL,
    range_km           INT NOT NULL,
    fuel_consumption   DECIMAL(8,2) NOT NULL
);

CREATE TABLE aircraft (
    aircraft_id      SERIAL PRIMARY KEY,
    model_id         INT NOT NULL REFERENCES aircraft_model(model_id),
    manufacture_date DATE NOT NULL
);

CREATE TABLE aircraft_status_history (
    history_id    SERIAL PRIMARY KEY,
    aircraft_id   INT NOT NULL REFERENCES aircraft(aircraft_id),
    status        aircraft_status_enum NOT NULL,
    status_time   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- flights...

CREATE TABLE route (
    route_id      SERIAL PRIMARY KEY,
    airport_from  INT NOT NULL REFERENCES airport(airport_id),
    airport_to    INT NOT NULL REFERENCES airport(airport_id),
    distance_km   INT NOT NULL
);

CREATE TABLE flight_schedule (
    schedule_id              SERIAL PRIMARY KEY,
    route_id                 INT NOT NULL REFERENCES route(route_id),
    scheduled_departure_time TIME NOT NULL,
    scheduled_arrival_time   TIME NOT NULL,
    days_of_week             weekday[] NOT NULL
);

-- instances


CREATE TABLE flight_instance (
    instance_id            SERIAL PRIMARY KEY,
    schedule_id            INT NOT NULL REFERENCES flight_schedule(schedule_id),
    scheduled_departure    TIMESTAMP NOT NULL,
    scheduled_arrival      TIMESTAMP NOT NULL,
    actual_departure       TIMESTAMP,
    actual_arrival         TIMESTAMP,
    status                 flight_status_enum NOT NULL DEFAULT 'Scheduled',
    aircraft_id            INT NOT NULL REFERENCES aircraft(aircraft_id),
    CONSTRAINT flight_instance_unique_aircraft UNIQUE (instance_id, aircraft_id)
);

-- users

CREATE TABLE customer (
    customer_id   SERIAL PRIMARY KEY,
    first_name    VARCHAR(50) NOT NULL,
    last_name     VARCHAR(50) NOT NULL,
    birth_date    DATE NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    phone         VARCHAR(20)
);

CREATE TABLE app_user (
    user_id        SERIAL PRIMARY KEY,
    username       VARCHAR(50) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           user_role NOT NULL DEFAULT 'USER',
    customer_id    INT REFERENCES customer(customer_id),
    CONSTRAINT app_user_customer_unique UNIQUE (customer_id)
);

-- tickets

CREATE TABLE booking (
    booking_id    SERIAL PRIMARY KEY,
    customer_id   INT NOT NULL REFERENCES customer(customer_id),
    booking_date  TIMESTAMP NOT NULL DEFAULT NOW(),
    status        booking_status_enum NOT NULL DEFAULT 'Pending'
);

CREATE TABLE fare (
    fare_id              SERIAL PRIMARY KEY,
    class                fare_class_enum NOT NULL,
    base_price           DECIMAL(10,2) NOT NULL,
    baggage_allowance_kg INT NOT NULL,
    rules                TEXT
);

CREATE TABLE seat_layout (
    aircraft_id   INT NOT NULL REFERENCES aircraft(aircraft_id),
    seat_id       VARCHAR(5) NOT NULL,
    seat_type     seat_type_enum NOT NULL,
    PRIMARY KEY (aircraft_id, seat_id)
);

CREATE TABLE ticket (
    ticket_id           SERIAL PRIMARY KEY,
    booking_id          INT NOT NULL REFERENCES booking(booking_id),
    flight_instance_id  INT NOT NULL REFERENCES flight_instance(instance_id),
    aircraft_id         INT NOT NULL,
    seat_id             VARCHAR(5) NOT NULL,
    fare_id             INT NOT NULL REFERENCES fare(fare_id),
    price               DECIMAL(10,2) NOT NULL,
    issue_date          DATE NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT fk_ticket_aircraft
        FOREIGN KEY (flight_instance_id, aircraft_id)
            REFERENCES flight_instance(instance_id, aircraft_id),
    CONSTRAINT fk_ticket_seat
        FOREIGN KEY (aircraft_id, seat_id)
            REFERENCES seat_layout(aircraft_id, seat_id)
);

-- payments

CREATE TABLE payment (
    payment_id       SERIAL PRIMARY KEY,
    ticket_id        INT NOT NULL REFERENCES ticket(ticket_id),
    amount           DECIMAL(10,2) NOT NULL,
    method           VARCHAR(20) NOT NULL,
    payment_datetime TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE baggage (
    baggage_id       SERIAL PRIMARY KEY,
    ticket_id        INT NOT NULL REFERENCES ticket(ticket_id),
    weight_kg        DECIMAL(5,2) NOT NULL,
    length_cm        INT,
    width_cm         INT,
    height_cm        INT
);

-- incident

CREATE TABLE incident (
    incident_id             SERIAL PRIMARY KEY,
    flight_instance_id      INT NOT NULL REFERENCES flight_instance(instance_id),
    incident_type           incident_type_enum NOT NULL,
    description             TEXT,
    departure_delay_minutes INT,
    arrival_delay_minutes   INT,
    occurred_at             TIMESTAMP NOT NULL DEFAULT NOW()
);

-- maintenance

CREATE TABLE maintenance (
    maintenance_id   SERIAL PRIMARY KEY,
    aircraft_id      INT NOT NULL REFERENCES aircraft(aircraft_id),
    start_date       TIMESTAMP NOT NULL,
    end_date         TIMESTAMP,
    description      TEXT,
    cost             DECIMAL(12,2)
);

--

CREATE TABLE review (
    review_id            SERIAL PRIMARY KEY,
    customer_id          INT NOT NULL REFERENCES customer(customer_id),
    flight_instance_id   INT NOT NULL REFERENCES flight_instance(instance_id),
    rating               SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 10),
    comment              TEXT,
    review_date          DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE OR REPLACE FUNCTION validate_customer_fields() RETURNS TRIGGER AS
$$
DECLARE
    age_years     INT;
    name_pattern  CONSTANT TEXT := '^[A-Za-z]+$';
    email_pattern CONSTANT TEXT := '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$';
    phone_pattern CONSTANT TEXT := '^[0-9]+$';
BEGIN
    IF NEW.first_name !~ name_pattern THEN
        RAISE EXCEPTION 'Invalid first_name: must contain only letters. Got "%" ', NEW.first_name;
    END IF;

    IF NEW.last_name !~ name_pattern THEN
        RAISE EXCEPTION 'Invalid last_name: must contain only letters. Got "%" ', NEW.last_name;
    END IF;

    age_years := DATE_PART('year', age(NOW(), NEW.birth_date));
    IF age_years < 16 THEN
        RAISE EXCEPTION 'Underage: must be at least 16 years old. Birth "%" (age %).',
                        NEW.birth_date, age_years;
    END IF;

    IF NEW.email !~ email_pattern THEN
        RAISE EXCEPTION 'Invalid email format: "%" ', NEW.email;
    END IF;

    IF NEW.phone IS NOT NULL AND NEW.phone <> '' THEN
        IF NEW.phone !~ phone_pattern THEN
            RAISE EXCEPTION 'Invalid phone: must contain only digits. Got "%" ', NEW.phone;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ language plpgsql;

DROP TRIGGER IF EXISTS validate_customer_trigger ON customer;
CREATE TRIGGER validate_customer_trigger BEFORE INSERT OR UPDATE ON customer FOR EACH ROW EXECUTE FUNCTION validate_customer_fields();
