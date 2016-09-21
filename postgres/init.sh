#!/bin/bash

# this script is runned when the docker container is built
# it imports the base database structure and create the database for the tests

DB=${DB_NAME-sandbox}

echo "*** CREATING DATABASE ${DB} ***"

# create default database
gosu postgres psql --user postgres <<EOSQL
  CREATE DATABASE ${DB};
  CREATE USER anonymous WITH PASSWORD 'pwd';
  GRANT ALL PRIVILEGES ON DATABASE ${DB} TO anonymous;
EOSQL

echo "*** DATABASE CREATED! ***"

echo "*** ADDING EXTENSIONS FOR DATABASE sandbox ***"

gosu postgres psql -d ${DB} --user postgres <<EOSQL
  CREATE EXTENSION tablefunc;
  CREATE EXTENSION pgcrypto;
EOSQL
