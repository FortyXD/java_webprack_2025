INSERT INTO currencys(name) VALUES
  ('RUB'), ('USD'), ('EUR')
ON CONFLICT (name) DO NOTHING;

WITH d AS (
  INSERT INTO departments(name, address, phone_number, email, longitude, latitude)
  VALUES
    ('Central Branch', 'Main st. 1', '+49-69-000-000', 'central@bank.local', 8.682127, 50.110924),
    ('North Branch',   'North st. 10', '+49-69-111-111', 'north@bank.local', 8.700000, 50.130000),
    ('South Branch',   'South st. 5', '+49-69-222-222', 'south@bank.local', 8.650000, 50.090000)
  RETURNING id, name
)
SELECT 1;

WITH dep AS (
  SELECT id, name FROM departments
),
c1 AS (
  INSERT INTO clients(department_id, type, surname, second_name, passport, snils, phone, address, link_to_photo)
  VALUES (
    (SELECT id FROM dep WHERE name='Central Branch'),
    'natural person',
    'Ivanov', 'Ivan', '4500 123456', '112-233-445 95', '+49-111-111', 'Frankfurt, Center', 'https://example.local/p/ivanov.jpg'
  ) RETURNING id
),
c2 AS (
  INSERT INTO clients(department_id, type, surname, second_name, passport, snils, phone, address, link_to_photo)
  VALUES (
    (SELECT id FROM dep WHERE name='North Branch'),
    'natural person',
    'Petrov', 'Petr', '4501 654321', '223-344-556 06', '+49-222-222', 'Frankfurt, North', NULL
  ) RETURNING id
),
c3 AS (
  INSERT INTO clients(department_id, type, surname, second_name, passport, snils, phone, address, link_to_photo)
  VALUES (
    (SELECT id FROM dep WHERE name='South Branch'),
    'natural person',
    'Smirnova', 'Anna', '4502 111222', '334-455-667 17', '+49-333-333', 'Frankfurt, South', NULL
  ) RETURNING id
),
c4 AS (
  INSERT INTO clients(department_id, type, surname, second_name, passport, snils, phone, address, link_to_photo)
  VALUES (
    (SELECT id FROM dep WHERE name='Central Branch'),
    'legal entity',
    'OOO "Romashka"', NULL, NULL, NULL, '+49-444-444', 'Frankfurt, Industrial zone', NULL
  ) RETURNING id
),
c5 AS (
  INSERT INTO clients(department_id, type, surname, second_name, passport, snils, phone, address, link_to_photo)
  VALUES (
    (SELECT id FROM dep WHERE name='North Branch'),
    'legal entity',
    'JSC "Tech"', NULL, NULL, NULL, '+49-555-555', 'Frankfurt, Business district', NULL
  ) RETURNING id
)
SELECT 1;

WITH cl AS (
  SELECT id FROM clients WHERE surname='Ivanov' AND second_name='Ivan' LIMIT 1
),
acc AS (
  INSERT INTO accounts(client_id, special_number, points, currency_id, status, account_type)
  VALUES (
    (SELECT id FROM cl),
    'ACC-0001',
    120,
    (SELECT id FROM currencys WHERE name='RUB'),
    'active',
    'credit'
  )
  RETURNING id
)
INSERT INTO credit(id, max_credit, current_dept, interest_rate, payment_method)
SELECT id, 200000.00, 50000.00, 12.5000, 'auto' FROM acc;

WITH cl AS (
  SELECT id FROM clients WHERE surname='Petrov' AND second_name='Petr' LIMIT 1
),
acc AS (
  INSERT INTO accounts(client_id, special_number, points, currency_id, status, account_type)
  VALUES (
    (SELECT id FROM cl),
    'ACC-0002',
    30,
    (SELECT id FROM currencys WHERE name='USD'),
    'active',
    'deposit'
  )
  RETURNING id
)
INSERT INTO deposit(id, initial_amount, end_date, automatic_renewal, payment_method)
SELECT id, 1500.00, (CURRENT_DATE + INTERVAL '180 days')::date, TRUE, 'manual' FROM acc;

WITH cl AS (
  SELECT id FROM clients WHERE surname='Smirnova' AND second_name='Anna' LIMIT 1
),
acc AS (
  INSERT INTO accounts(client_id, special_number, points, currency_id, status, account_type)
  VALUES (
    (SELECT id FROM cl),
    'ACC-0003',
    5,
    (SELECT id FROM currencys WHERE name='RUB'),
    'active',
    'saving'
  )
  RETURNING id
)
INSERT INTO saving_account(id, interest_rate, max_limit)
SELECT id, 7.2000, 1000000.00 FROM acc;

WITH cl AS (
  SELECT id FROM clients WHERE surname='OOO "Romashka"' LIMIT 1
),
acc AS (
  INSERT INTO accounts(client_id, special_number, points, currency_id, status, account_type)
  VALUES (
    (SELECT id FROM cl),
    'ACC-0004',
    0,
    (SELECT id FROM currencys WHERE name='EUR'),
    'active',
    'saving'
  )
  RETURNING id
)
INSERT INTO saving_account(id, interest_rate, max_limit)
SELECT id, 3.5000, 5000000.00 FROM acc;

WITH cl AS (
  SELECT id FROM clients WHERE surname='JSC "Tech"' LIMIT 1
),
acc AS (
  INSERT INTO accounts(client_id, special_number, points, currency_id, status, account_type)
  VALUES (
    (SELECT id FROM cl),
    'ACC-0005',
    900,
    (SELECT id FROM currencys WHERE name='RUB'),
    'active',
    'deposit'
  )
  RETURNING id
)
INSERT INTO deposit(id, initial_amount, end_date, automatic_renewal, payment_method)
SELECT id, 250000.00, (CURRENT_DATE + INTERVAL '365 days')::date, FALSE, 'auto' FROM acc;

WITH cl AS (
  SELECT id FROM clients WHERE surname='Petrov' AND second_name='Petr' LIMIT 1
),
acc AS (
  INSERT INTO accounts(client_id, special_number, points, currency_id, status, account_type)
  VALUES (
    (SELECT id FROM cl),
    'ACC-0006',
    10,
    (SELECT id FROM currencys WHERE name='USD'),
    'suspended',
    'credit'
  )
  RETURNING id
)
INSERT INTO credit(id, max_credit, current_dept, interest_rate, payment_method)
SELECT id, 10000.00, 1200.00, 19.9000, 'manual' FROM acc;

INSERT INTO account_operations(account_id, kind, amount, description)
SELECT a.id, 'credit', 500.00, 'Initial deposit' FROM accounts a WHERE a.special_number = 'ACC-0001';
INSERT INTO account_operations(account_id, kind, amount, description)
SELECT a.id, 'debit', 380.00, 'Payment' FROM accounts a WHERE a.special_number = 'ACC-0001';

INSERT INTO account_operations(account_id, kind, amount, description)
SELECT a.id, 'credit', 1500.00, 'Opening deposit' FROM accounts a WHERE a.special_number = 'ACC-0002';