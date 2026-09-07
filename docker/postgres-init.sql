CREATE USER inventory WITH PASSWORD 'inventory';
CREATE DATABASE inventory OWNER inventory;
\connect inventory
GRANT ALL ON SCHEMA public TO inventory;

CREATE USER reservation WITH PASSWORD 'reservation';
CREATE DATABASE reservation OWNER reservation;
\connect reservation
GRANT ALL ON SCHEMA public TO reservation;
