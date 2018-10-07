CREATE TABLE users (
  user_id UUID PRIMARY KEY,
  name    Text NOT NULL,
  surname Text NOT NULL
);

CREATE TABLE entitlements (
  user_id     UUID NOT NULL REFERENCES users,
  resource_id UUID NOT NULL,
  level       Text NOT NULL
);

CREATE TABLE journal (
  service_name Text NOT NULL,
  call_id      Text NOT NULL PRIMARY KEY,
  stage_no     Int  NOT NULL,
  argument     Text NOT NULL,
  status       Text NOT NULL
);
