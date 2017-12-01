DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS iplist;

CREATE TABLE messages
  (
     id        IDENTITY PRIMARY KEY,
     digest    VARCHAR(255),
     publickey VARCHAR(255),
     timestamp BIGINT,
     msg       VARCHAR(255)
  );

CREATE TABLE iplist
  (
     publickey VARCHAR(255),
     ipaddress INT,
     timestamp BIGINT,
     count     INT
  );
