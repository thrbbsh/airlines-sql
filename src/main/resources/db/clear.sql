
BEGIN;

-- triggers
DROP TRIGGER IF EXISTS validate_customer_trigger ON customer;
DROP TRIGGER IF EXISTS trigger_validate_aircraft_date ON aircraft;
DROP TRIGGER IF EXISTS trg_simulate_flight_instance ON flight_instance;
DROP TRIGGER IF EXISTS trg_check_aircraft_timing ON flight_instance;
DROP TRIGGER IF EXISTS trg_create_tickets_by_row_class ON flight_instance;
DROP TRIGGER IF EXISTS set_default_real_price ON flight_instance;
DROP TRIGGER IF EXISTS trg_schedule_validity ON flight_instance;
DROP TRIGGER IF EXISTS trg_validate_seat_for_aircraft_model ON ticket;
DROP TRIGGER IF EXISTS trg_set_initial_aircraft_status ON aircraft;

-- functions
DROP FUNCTION IF EXISTS validate_customer_fields() CASCADE;
DROP FUNCTION IF EXISTS validate_aircraft_date() CASCADE;

DROP FUNCTION IF EXISTS get_aircraft_current_status(INT) CASCADE;
DROP FUNCTION IF EXISTS set_aircraft_status(INT, aircraft_status_enum) CASCADE;

DROP FUNCTION IF EXISTS set_flight_status(INT, flight_status_enum) CASCADE;
DROP FUNCTION IF EXISTS get_flight_current_status(INT) CASCADE;

DROP FUNCTION IF EXISTS set_aircraft_location(INT, INT, TIMESTAMP) CASCADE;
DROP FUNCTION IF EXISTS get_aircraft_current_airport(INT) CASCADE;

DROP FUNCTION IF EXISTS simulate_flight_instance_events() CASCADE;

DROP FUNCTION IF EXISTS trg_set_default_real_price() CASCADE;

DROP FUNCTION IF EXISTS create_tickets_by_row_class() CASCADE;
DROP FUNCTION IF EXISTS check_aircraft_timing() CASCADE;

DROP FUNCTION IF EXISTS trg_check_schedule_validity() CASCADE;
DROP FUNCTION IF EXISTS validate_seat_for_aircraft_model() CASCADE;
DROP FUNCTION IF EXISTS handle_major_incident() CASCADE;
DROP FUNCTION IF EXISTS set_initial_aircraft_status() CASCADE;

-- tables
DROP TABLE IF EXISTS aircraft_location_history CASCADE;
DROP TABLE IF EXISTS flight_status_history CASCADE;
DROP TABLE IF EXISTS maintenance CASCADE;
DROP TABLE IF EXISTS incident CASCADE;
DROP TABLE IF EXISTS booking CASCADE;
DROP TABLE IF EXISTS ticket CASCADE;
DROP TABLE IF EXISTS seat_layout CASCADE;
DROP TABLE IF EXISTS fare CASCADE;
DROP TABLE IF EXISTS flight_instance CASCADE;
DROP TABLE IF EXISTS flight_schedule CASCADE;
DROP TABLE IF EXISTS route CASCADE;
DROP TABLE IF EXISTS aircraft_status_history CASCADE;
DROP TABLE IF EXISTS aircraft CASCADE;
DROP TABLE IF EXISTS aircraft_model CASCADE;
DROP TABLE IF EXISTS airport CASCADE;
DROP TABLE IF EXISTS city CASCADE;
DROP TABLE IF EXISTS country CASCADE;
DROP TABLE IF EXISTS app_user CASCADE;
DROP TABLE IF EXISTS customer CASCADE;

-- enum's
DROP TYPE IF EXISTS weekday CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS flight_status_enum CASCADE;
DROP TYPE IF EXISTS aircraft_status_enum CASCADE;
DROP TYPE IF EXISTS booking_status_enum CASCADE;
DROP TYPE IF EXISTS seat_type_enum CASCADE;
DROP TYPE IF EXISTS incident_type_enum CASCADE;
DROP TYPE IF EXISTS fare_class_enum CASCADE;

COMMIT;