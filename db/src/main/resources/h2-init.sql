DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS iplist;

CREATE TABLE messages
  (
     sourceid      VARCHAR(255),
     message       VARCHAR(255),
     messagedigest BINARY(128),
     timestamp     BIGINT,
     publickey     BINARY(1024),
     signature     BINARY(1024),
     PRIMARY KEY(sourceid, messagedigest, timestamp)
  );

CREATE TABLE iplist
  (
     publickey VARCHAR(255),
     ipaddress INT,
     timestamp BIGINT,
     count     INT
  );
