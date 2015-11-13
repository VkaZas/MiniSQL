CREATE TABLE orders (
  orderkey    INT,
  custkey     INT UNIQUE,
  orderstatus CHAR(1),
  totalprice  FLOAT,
  clerk       CHAR(15),
  comments    CHAR(79) UNIQUE,
  PRIMARY KEY (orderkey)
);