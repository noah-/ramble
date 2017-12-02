DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS iplist;

CREATE TABLE messages
  (
     digest    VARCHAR(255),
     publickey VARCHAR(255),
     timestamp BIGINT,
     msg       VARCHAR(255),
     PRIMARY KEY(digest, publickey, timestamp)
  );

CREATE TABLE iplist
  (
     publickey VARCHAR(255),
     ipaddress INT,
     timestamp BIGINT,
     count     INT
  );
