COPY country (country_id, name, iso_code) FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/countries.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('country', 'country_id'), (SELECT MAX(country_id) FROM country));
--
COPY city (city_id, name, country_id) FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/city.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('city', 'city_id'), (SELECT MAX(city_id) FROM city));
--
COPY airport (airport_id, name, airport_iata, city_id) FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/airports.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('airport', 'airport_id'), (SELECT MAX(airport_id) FROM airport));
--

COPY aircraft_model FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/aircraft_model.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('aircraft_model', 'model_id'), (SELECT MAX(model_id) FROM aircraft_model));
--
COPY seat_layout FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/seat_layout.csv' WITH (FORMAT csv, HEADER true);
--
COPY fare FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/fare.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('fare', 'fare_id'), (SELECT MAX(fare_id) FROM fare));
--
COPY aircraft FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/aircraft.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('aircraft', 'aircraft_id'), (SELECT MAX(aircraft_id) FROM aircraft));
--
-- COPY aircraft_status_history FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/aircraft_status_history.csv' WITH (FORMAT csv, HEADER true);
--
-- SELECT setval(pg_get_serial_sequence('aircraft_status_history', 'history_id'), (SELECT MAX(history_id) FROM aircraft_status_history));
--
CREATE INDEX idx_flight_status_history_instance_id_time_desc ON flight_status_history (instance_id, status_time DESC);
--
COPY route FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/route.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('route', 'route_id'), (SELECT MAX(route_id) FROM route));
--
COPY flight_schedule (schedule_id, route_id, scheduled_departure_time, scheduled_arrival_time, day_of_week)
FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/schedule.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('flight_schedule', 'schedule_id'), (SELECT MAX(schedule_id) FROM flight_schedule));
--
COPY flight_instance FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/flight_instance.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('flight_instance', 'instance_id'), (SELECT MAX(instance_id) FROM flight_instance));
--
COPY customer FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/customers.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'), (SELECT MAX(customer_id) FROM customer));
--
COPY app_user FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/app_user.csv' WITH (FORMAT csv, HEADER true);
--
SELECT setval(pg_get_serial_sequence('app_user', 'user_id'), (SELECT MAX(user_id) FROM app_user));
--
-- COPY booking FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/booking.csv' WITH (FORMAT csv, HEADER true);
--
-- SELECT setval(pg_get_serial_sequence('booking', 'booking_id'), (SELECT MAX(booking_id) FROM booking));
--
-- COPY ticket FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/ticket.csv' WITH (FORMAT csv, HEADER true);
--
-- SELECT setval(pg_get_serial_sequence('ticket', 'ticket_id'), (SELECT MAX(ticket_id) FROM ticket));
--
-- COPY payment FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/payment.csv' WITH (FORMAT csv, HEADER true);
--
-- SELECT setval(pg_get_serial_sequence('payment', 'payment_id'), (SELECT MAX(payment_id) FROM payment));
--
-- COPY baggage FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/baggage.csv' WITH (FORMAT csv, HEADER true);
--
-- SELECT setval(pg_get_serial_sequence('baggage', 'baggage_id'), (SELECT MAX(baggage_id) FROM baggage));
--
-- COPY incident FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/incident.csv' WITH (FORMAT csv, HEADER true);
--
-- SELECT setval(pg_get_serial_sequence('incident', 'incident_id'), (SELECT MAX(incident_id) FROM incident));
--
-- COPY maintenance FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/maintenance.csv' WITH (FORMAT csv, HEADER true);
--
-- SELECT setval(pg_get_serial_sequence('maintenance', 'maintenance_id'), (SELECT MAX(maintenance_id) FROM maintenance));
--
-- COPY review FROM '/Users/volochai/prog/airlines-sql/src/main/resources/db/review.csv' WITH (FORMAT csv, HEADER true);
--
-- SELECT setval(pg_get_serial_sequence('review', 'review_id'), (SELECT MAX(review_id) FROM review));
--