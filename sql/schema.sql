
DO $$ BEGIN
  CREATE TYPE payment_method AS ENUM ('auto', 'manual');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE status AS ENUM ('active', 'closed', 'suspended');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE account_type AS ENUM ('credit', 'deposit', 'saving');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE client_type AS ENUM ('legal entity', 'natural person');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS departments (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name          TEXT NOT NULL,
  address       TEXT,
  phone_number  TEXT,
  email         TEXT,
  longitude     NUMERIC(9,6),
  latitude      NUMERIC(9,6)
);

CREATE TABLE IF NOT EXISTS clients (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  department_id BIGINT NOT NULL REFERENCES departments(id),
  type          client_type NOT NULL,
  count         INTEGER,
  surname       TEXT,
  second_name   TEXT,
  passport      TEXT,
  snils         TEXT,
  phone         TEXT,
  address       TEXT,
  link_to_photo TEXT,
  CONSTRAINT clients_passport_uk UNIQUE (passport),
  CONSTRAINT clients_snils_uk    UNIQUE (snils)
);

CREATE TABLE IF NOT EXISTS currencys (
  id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name  TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS accounts (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  client_id      BIGINT NOT NULL REFERENCES clients(id),
  special_number TEXT NOT NULL UNIQUE,
  points         INTEGER NOT NULL DEFAULT 0,
  currency_id    BIGINT NOT NULL REFERENCES currencys(id),
  status         status NOT NULL DEFAULT 'active',
  account_type   account_type NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS credit (
  id             BIGINT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  max_credit     NUMERIC(14,2) NOT NULL,
  current_dept   NUMERIC(14,2) NOT NULL DEFAULT 0,
  interest_rate  NUMERIC(8,4)  NOT NULL,
  payment_method payment_method NOT NULL
);

CREATE TABLE IF NOT EXISTS deposit (
  id                BIGINT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  initial_amount    NUMERIC(14,2) NOT NULL,
  end_date          DATE NOT NULL,
  automatic_renewal BOOLEAN NOT NULL DEFAULT FALSE,
  payment_method    payment_method NOT NULL
);

CREATE TABLE IF NOT EXISTS "savings account" (
  id            BIGINT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  interest_rate NUMERIC(8,4)  NOT NULL,
  max_limit     NUMERIC(14,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_clients_department_id ON clients(department_id);
CREATE INDEX IF NOT EXISTS idx_accounts_client_id     ON accounts(client_id);
CREATE INDEX IF NOT EXISTS idx_accounts_currency_id   ON accounts(currency_id);