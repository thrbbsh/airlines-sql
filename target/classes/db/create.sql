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
    'Landed',
    'Crashed'
);

CREATE TYPE aircraft_status_enum AS ENUM (
    'InService',
    'UnderMaintenance',
    'Retired',  -- we have an airplane like this in stock
    'WrittenOff', -- burned/dropped (lost such an aircraft)
    'Storage' -- still works
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
    'Middle'
);

CREATE TYPE incident_type_enum AS ENUM (
    'Technical',
    'Weather',
    'Security',
    'Catastrophe',
    'DestroyedOnGround',
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
    airport_iata VARCHAR(3) NOT NULL,
    city_id      INT NOT NULL REFERENCES city(city_id)
);

-- aircrafts

CREATE TABLE aircraft_model (
    model_id           SERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    passenger_count    INT NOT NULL,
    range_km           INT NOT NULL
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

CREATE TABLE aircraft_location_history (
    history_id    SERIAL      PRIMARY KEY,
    aircraft_id   INT         NOT NULL REFERENCES aircraft(aircraft_id),
    airport_id    INT         NOT NULL REFERENCES airport(airport_id),
    recorded_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- flights...

CREATE TABLE route (
    route_id      SERIAL PRIMARY KEY,
    airport_from  INT NOT NULL REFERENCES airport(airport_id),
    airport_to    INT NOT NULL REFERENCES airport(airport_id),
    distance_km   INT NOT NULL,
    default_price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT chk_different_airports CHECK (airport_from <> airport_to)
);

CREATE TABLE flight_schedule (
    schedule_id              SERIAL PRIMARY KEY,
    route_id                 INT    NOT NULL REFERENCES route(route_id),
    scheduled_departure_time TIME   NOT NULL,
    scheduled_arrival_time   TIME   NOT NULL,
    day_of_week              weekday NOT NULL,
    valid_from               DATE   NOT NULL DEFAULT '1970-01-01',
    valid_to                 DATE   NOT NULL DEFAULT '9999-12-31'
);

-- instances

CREATE TABLE flight_instance (
    instance_id            SERIAL PRIMARY KEY,
    schedule_id            INT    NOT NULL          REFERENCES flight_schedule(schedule_id),
    scheduled_departure    TIMESTAMP NOT NULL,
    scheduled_arrival      TIMESTAMP NOT NULL,
    actual_departure       TIMESTAMP,
    actual_arrival         TIMESTAMP,
    aircraft_id            INT REFERENCES aircraft(aircraft_id),
    real_price             DECIMAL(10, 2)
);

CREATE TABLE flight_status_history (
    history_id  SERIAL PRIMARY KEY,
    instance_id INT NOT NULL REFERENCES flight_instance(instance_id),
    status      flight_status_enum NOT NULL,
    status_time TIMESTAMP NOT NULL DEFAULT NOW()
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

CREATE TABLE fare (
    fare_id                 SERIAL PRIMARY KEY,
    class                   fare_class_enum NOT NULL,
    class_multiplier        DECIMAL(10,2) NOT NULL
);

CREATE TABLE seat_layout (
    model_id     INT NOT NULL REFERENCES aircraft_model(model_id),
    seat_id      VARCHAR(5) NOT NULL,
    seat_type    seat_type_enum NOT NULL,
    PRIMARY KEY  (model_id, seat_id)
);

CREATE TABLE ticket (
    ticket_id           SERIAL        PRIMARY KEY,
    instance_id         INT           NOT NULL   REFERENCES flight_instance(instance_id),
    seat_id             VARCHAR(5)    NOT NULL,
    fare_id             INT           NOT NULL   REFERENCES fare(fare_id),
    price               DECIMAL(10,2) NOT NULL,

    UNIQUE (instance_id, seat_id)
);

CREATE TABLE booking (
     booking_id          SERIAL PRIMARY KEY,
     customer_id         INT                 NOT NULL REFERENCES customer(customer_id),
     booking_date        TIMESTAMP           NOT NULL DEFAULT NOW(),
     status              booking_status_enum NOT NULL DEFAULT 'Pending',
     ticket_id           INT                 NOT NULL REFERENCES ticket(ticket_id)
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

CREATE OR REPLACE FUNCTION validate_customer_fields() RETURNS TRIGGER AS
$$
DECLARE
    name_pattern CONSTANT TEXT := '^[A-Za-z]+([ -][A-Za-z]+)*$';
    email_pattern CONSTANT TEXT := '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$';
    phone_pattern CONSTANT TEXT := '^[+]?[0-9]{10,15}$';
BEGIN
    IF NEW.first_name !~ name_pattern THEN
        RAISE EXCEPTION 'Invalid first_name: must contain only letters. Got "%" ', NEW.first_name;
    END IF;

    IF NEW.last_name !~ name_pattern THEN
        RAISE EXCEPTION 'Invalid last_name: must contain only letters. Got "%" ', NEW.last_name;
    END IF;

    IF NEW.birth_date >= CURRENT_DATE THEN
       RAISE EXCEPTION 'Incorrect birth date.';
    END IF;

    IF NEW.email !~ email_pattern THEN
        RAISE EXCEPTION 'Invalid email format: "%" ', NEW.email;
    END IF;

    IF NEW.phone IS NOT NULL AND NEW.phone <> '' THEN
        IF NEW.phone !~ phone_pattern THEN
            RAISE EXCEPTION 'Invalid phone: must contain only 10 to 15 digits. Got "%" ', NEW.phone;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ language plpgsql;

CREATE TRIGGER validate_customer_trigger BEFORE INSERT OR UPDATE ON customer FOR EACH ROW EXECUTE FUNCTION validate_customer_fields();

CREATE OR REPLACE FUNCTION validate_aircraft_date() RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.manufacture_date >= CURRENT_DATE THEN
        RAISE EXCEPTION 'Aircraft production date (manufacture_date) cannot be in the future: %', NEW.manufacture_date;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_aircraft_date BEFORE INSERT OR UPDATE ON aircraft FOR EACH ROW EXECUTE FUNCTION validate_aircraft_date();

CREATE OR REPLACE FUNCTION get_aircraft_current_status(p_aircraft_id INT) RETURNS aircraft_status_enum AS
$$
DECLARE
       v_status aircraft_status_enum;
BEGIN
    SELECT ash.status INTO v_status
    FROM aircraft_status_history ash
    WHERE ash.aircraft_id = p_aircraft_id
    ORDER BY ash.status_time DESC LIMIT 1;

    RETURN v_status;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_aircraft_status(p_instance_id INT, p_status aircraft_status_enum) RETURNS VOID AS
$$
BEGIN
    INSERT INTO aircraft_status_history (instance_id, status)
    VALUES (p_instance_id, p_status);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_flight_current_status(p_inst_id INT) RETURNS flight_status_enum AS $$
DECLARE
    v_status flight_status_enum;
BEGIN
    SELECT fsh.status INTO v_status
    FROM flight_status_history fsh
    WHERE fsh.instance_id = p_inst_id
    ORDER BY fsh.status_time DESC LIMIT 1;

    RETURN v_status;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_flight_status(p_inst_id INT, p_new_status flight_status_enum) RETURNS VOID AS
$$
BEGIN
    INSERT INTO flight_status_history(instance_id, status)
    VALUES (p_inst_id, p_new_status);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_aircraft_location(p_aircraft_id INT, p_airport_id INT, p_recorded_at TIMESTAMP) RETURNS VOID AS $$
BEGIN
    INSERT INTO aircraft_location_history(aircraft_id, airport_id, recorded_at)
    VALUES (p_aircraft_id, p_airport_id, p_recorded_at);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_aircraft_current_airport(p_aircraft_id INT) RETURNS INT AS
$$
DECLARE
    v_airport_id INT;
BEGIN
    SELECT airport_id INTO v_airport_id
    FROM aircraft_location_history
    WHERE aircraft_id = p_aircraft_id
    ORDER BY recorded_at DESC LIMIT 1;

    RETURN v_airport_id;
END;
$$ LANGUAGE plpgsql;

-- just simulation of already occurred flights when loading the base actual at some point in time
-- in the real world, the status of all flights is managed by air traffic control.
CREATE OR REPLACE FUNCTION simulate_flight_instance_events() RETURNS TRIGGER AS
$$
DECLARE
    v_now       TIMESTAMP := NOW();
    v_sched_ts  TIMESTAMP;
    v_depart    TIMESTAMP := NEW.scheduled_departure;
    v_arrive    TIMESTAMP := NEW.scheduled_arrival;
    v_dest_airport INT;
BEGIN
    v_sched_ts := LEAST(v_now, v_depart - INTERVAL '2 days');
    INSERT INTO flight_status_history(instance_id, status, status_time)
    VALUES (NEW.instance_id, 'Scheduled', v_sched_ts);

    IF v_now >= v_depart THEN
            INSERT INTO flight_status_history(instance_id, status, status_time)
            VALUES (NEW.instance_id, 'Departed', v_depart);
    END IF;

    IF v_now >= v_arrive THEN
        INSERT INTO flight_status_history(instance_id, status, status_time)
        VALUES (NEW.instance_id, 'Landed', v_arrive);

        SELECT r.airport_to INTO v_dest_airport
        FROM flight_schedule fs
        JOIN route r ON fs.route_id = r.route_id
        WHERE fs.schedule_id = NEW.schedule_id;

        PERFORM set_aircraft_location(NEW.aircraft_id, v_dest_airport, v_arrive);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_simulate_flight_instance AFTER INSERT ON flight_instance FOR EACH ROW EXECUTE FUNCTION simulate_flight_instance_events();

CREATE OR REPLACE FUNCTION trg_set_default_real_price() RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.real_price IS NULL OR NEW.real_price <= 0 THEN
        SELECT r.default_price INTO NEW.real_price FROM flight_schedule fs
        JOIN route r ON fs.route_id = r.route_id
        WHERE fs.schedule_id = NEW.schedule_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_default_real_price BEFORE INSERT ON flight_instance FOR EACH ROW EXECUTE FUNCTION trg_set_default_real_price();

-- lol
CREATE OR REPLACE FUNCTION create_tickets_by_row_class() RETURNS trigger AS
$$
BEGIN
    IF NEW.scheduled_departure > now() THEN
        INSERT INTO ticket (instance_id, seat_id, fare_id, price)
        SELECT NEW.instance_id, sl.seat_id, f.fare_id, ROUND(r.default_price * f.class_multiplier, 2) AS price
        FROM (
            SELECT sl.*, (regexp_replace(sl.seat_id, '[^0-9]', '', 'g'))::INT AS row_num
            FROM seat_layout AS sl WHERE sl.model_id = (SELECT model_id FROM aircraft WHERE aircraft_id = NEW.aircraft_id)
     ) AS sl
         JOIN flight_schedule AS fs ON fs.schedule_id = NEW.schedule_id
         JOIN route AS r ON r.route_id = fs.route_id
         JOIN fare AS f
              ON f.class = CASE
                               WHEN sl.row_num = 1        THEN 'First'::fare_class_enum
                               WHEN sl.row_num IN (2, 3)  THEN 'Business'::fare_class_enum
                               ELSE 'Economy'::fare_class_enum
                  END
    ON CONFLICT (instance_id, seat_id) DO NOTHING;

    RAISE NOTICE 'Tickets created by row rules for flight instance %', NEW.instance_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_create_tickets_by_row_class AFTER INSERT ON flight_instance FOR EACH ROW EXECUTE FUNCTION create_tickets_by_row_class();

CREATE OR REPLACE FUNCTION check_aircraft_timing() RETURNS TRIGGER AS
$$
DECLARE
    v_speed        NUMERIC        := 5000;               -- just approximately
    v_turnaround   INTERVAL       := INTERVAL '0 hour'; -- rest

    prev_arrival         TIMESTAMP;
    prev_dest_airport    INT;
    new_dep_airport      INT;
    dist_prev            NUMERIC;
    ferry_time_prev      INTERVAL;

    next_departure       TIMESTAMP;
    next_route_airport   INT;
    dist_next            NUMERIC;
    ferry_time_next      INTERVAL;
BEGIN
    SELECT r.airport_from, r.airport_to
    INTO new_dep_airport, prev_dest_airport
    FROM flight_schedule fs
    JOIN route r ON fs.route_id = r.route_id
    WHERE fs.schedule_id = NEW.schedule_id;

    SELECT fi.scheduled_arrival, r.airport_to
    INTO prev_arrival, prev_dest_airport
    FROM flight_instance fi
    JOIN flight_schedule fs ON fi.schedule_id = fs.schedule_id
    JOIN route r ON fs.route_id = r.route_id
    WHERE fi.aircraft_id = NEW.aircraft_id
    AND fi.scheduled_arrival < NEW.scheduled_departure
    ORDER BY fi.scheduled_arrival DESC
    LIMIT 1;

    IF FOUND THEN
        SELECT distance_km
        INTO dist_prev
        FROM route
        WHERE airport_from = prev_dest_airport
        AND airport_to = new_dep_airport;

        IF NOT FOUND AND prev_dest_airport <> new_dep_airport THEN
            dist_prev := 0; -- we are in the same city
        END IF;

            ferry_time_prev := make_interval(secs => (dist_prev / v_speed) * 3600)::INTERVAL + v_turnaround;
            IF prev_arrival + ferry_time_prev > NEW.scheduled_departure THEN
                RAISE EXCEPTION
                  'Aircraft % will not make it from the previous flight (% + % = %) to the new departure on time %',
                  NEW.aircraft_id, prev_arrival, ferry_time_prev,
                  prev_arrival + ferry_time_prev, NEW.scheduled_departure;
        END IF;
    END IF;

    SELECT fi.scheduled_departure, fs.route_id
    INTO next_departure, next_route_airport
    FROM flight_instance fi
    JOIN flight_schedule fs ON fi.schedule_id = fs.schedule_id
    WHERE fi.aircraft_id = NEW.aircraft_id
    AND fi.scheduled_departure > NEW.scheduled_arrival
    ORDER BY fi.scheduled_departure
    LIMIT 1;

    IF FOUND THEN
        SELECT airport_from
        INTO next_route_airport
        FROM route
        WHERE route_id = next_route_airport;

        SELECT distance_km
        INTO dist_next
        FROM route
        WHERE airport_from = prev_dest_airport
        AND airport_to = next_route_airport;

        IF NOT FOUND AND prev_dest_airport <> next_route_airport THEN
            dist_prev := 0; -- we are in the same city
        END IF;

        ferry_time_next := make_interval(secs => (dist_next / v_speed) * 3600)::INTERVAL + v_turnaround;
        IF NEW.scheduled_arrival + ferry_time_next > next_departure THEN
            RAISE EXCEPTION
              'Aircraft % will not make it from the previous flight (% + % = %) to the new departure on time %',
              NEW.aircraft_id, NEW.scheduled_arrival, ferry_time_next,
              NEW.scheduled_arrival + ferry_time_next, next_departure;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_aircraft_timing BEFORE INSERT OR UPDATE ON flight_instance FOR EACH ROW EXECUTE FUNCTION check_aircraft_timing();

CREATE OR REPLACE FUNCTION trg_check_schedule_validity() RETURNS TRIGGER AS
$$
DECLARE
    v_valid_from  DATE;
    v_valid_to    DATE;
    v_sched_time  TIME;
    dep_date      DATE;
    dep_time      TIME;
BEGIN
    SELECT valid_from, valid_to, scheduled_departure_time
    INTO v_valid_from, v_valid_to, v_sched_time
    FROM flight_schedule
    WHERE schedule_id = NEW.schedule_id;

    dep_date := NEW.scheduled_departure::date;
    dep_time := NEW.scheduled_departure::time;

    IF dep_date < v_valid_from OR dep_date > v_valid_to THEN
        RAISE EXCEPTION
          'Flight date % is outside schedule [% - %]',
          dep_date, v_valid_from, v_valid_to;
    END IF;

    IF dep_time <> v_sched_time THEN
        RAISE EXCEPTION
          'Flight time % does not match schedule departure time %',
          dep_time, v_sched_time;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_schedule_validity BEFORE INSERT OR UPDATE ON flight_instance FOR EACH ROW EXECUTE FUNCTION trg_check_schedule_validity();

CREATE OR REPLACE FUNCTION validate_seat_for_aircraft_model() RETURNS TRIGGER AS
$$
DECLARE
v_model_id INT;
    v_seat_exists BOOLEAN;
BEGIN
    SELECT a.model_id
    INTO v_model_id
    FROM flight_instance fi
             JOIN aircraft a ON fi.aircraft_id = a.aircraft_id
    WHERE fi.instance_id = NEW.instance_id;

    SELECT EXISTS (
        SELECT 1
        FROM seat_layout sl
        WHERE sl.model_id = v_model_id AND sl.seat_id = NEW.seat_id
    )
    INTO v_seat_exists;

    IF NOT v_seat_exists THEN
        RAISE EXCEPTION 'Seat ''%'' does not exist on aircraft model (ID: %) for this flight.',
            NEW.seat_id, v_model_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_seat_for_aircraft_model BEFORE INSERT ON ticket FOR EACH ROW EXECUTE FUNCTION validate_seat_for_aircraft_model();

CREATE OR REPLACE FUNCTION handle_major_incident() RETURNS TRIGGER AS
$$
DECLARE
    v_aircraft_id INT;
BEGIN
    IF NEW.incident_type IN ('Catastrophe', 'DestroyedOnGround') THEN

    SELECT aircraft_id INTO v_aircraft_id
    FROM flight_instance
    WHERE instance_id = NEW.flight_instance_id;

    IF v_aircraft_id IS NULL THEN
        RAISE WARNING 'Major incident logged for flight_instance_id %, but no aircraft was assigned. No further actions taken.', NEW.flight_instance_id;
    RETURN NEW;
    END IF;

    RAISE NOTICE 'MAJOR INCIDENT DETECTED! Aircraft ID: %. Starting automated response...', v_aircraft_id;

    IF NEW.incident_type = 'Catastrophe' THEN
        PERFORM set_flight_status(NEW.flight_instance_id, 'Crashed');
        RAISE NOTICE '  -> Flight instance % status set to Crashed.', NEW.flight_instance_id;
    END IF;

    INSERT INTO aircraft_status_history (aircraft_id, status)
    VALUES (v_aircraft_id, 'WrittenOff');
    RAISE NOTICE '  -> Aircraft % status set to WrittenOff.', v_aircraft_id;

    UPDATE flight_instance
    SET aircraft_id = NULL
    WHERE aircraft_id = v_aircraft_id
    AND scheduled_departure > NOW();

    RAISE NOTICE '  -> All future flights unassigned from aircraft %.', v_aircraft_id;

    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_handle_major_incident AFTER INSERT ON incident FOR EACH ROW EXECUTE FUNCTION handle_major_incident();

CREATE OR REPLACE FUNCTION set_initial_aircraft_status() RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO aircraft_status_history (aircraft_id, status)
    VALUES (NEW.aircraft_id, 'InService');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_initial_aircraft_status AFTER INSERT ON aircraft FOR EACH ROW EXECUTE FUNCTION set_initial_aircraft_status();