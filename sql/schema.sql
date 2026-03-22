
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
  type          TEXT NOT NULL CHECK (type IN ('legal entity', 'natural person')),
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
  points         FLOAT NOT NULL DEFAULT 0,
  currency_id    BIGINT NOT NULL REFERENCES currencys(id),
  status         TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'closed', 'suspended')),
  account_type   TEXT NOT NULL CHECK (account_type IN ('credit', 'deposit', 'saving')),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  closed_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS credit (
  id             BIGINT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  max_credit     NUMERIC(14,2) NOT NULL,
  current_dept   NUMERIC(14,2) NOT NULL DEFAULT 0,
  interest_rate  NUMERIC(8,4)  NOT NULL,
  payment_method TEXT NOT NULL CHECK (payment_method IN ('auto', 'manual'))
);

CREATE TABLE IF NOT EXISTS deposit (
  id                BIGINT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  initial_amount    NUMERIC(14,2) NOT NULL,
  end_date          DATE NOT NULL,
  automatic_renewal BOOLEAN NOT NULL DEFAULT FALSE,
  payment_method    TEXT NOT NULL CHECK (payment_method IN ('auto', 'manual'))
);

CREATE TABLE IF NOT EXISTS saving_account (
  id            BIGINT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  interest_rate NUMERIC(8,4)  NOT NULL,
  max_limit     NUMERIC(14,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS account_operations (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  account_id  BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  kind        TEXT NOT NULL CHECK (kind IN ('credit', 'debit')),
  amount      NUMERIC(14,2) NOT NULL,
  description TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_clients_department_id ON clients(department_id);
CREATE INDEX IF NOT EXISTS idx_accounts_client_id     ON accounts(client_id);
CREATE INDEX IF NOT EXISTS idx_accounts_currency_id   ON accounts(currency_id);
CREATE INDEX IF NOT EXISTS idx_account_operations_account_id ON account_operations(account_id);