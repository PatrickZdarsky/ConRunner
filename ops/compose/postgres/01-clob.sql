-- Some legacy entities declare CLOB columns; PostgreSQL stores them as text.
CREATE DOMAIN clob AS text;
