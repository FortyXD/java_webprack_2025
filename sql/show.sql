SELECT * FROM departments ORDER BY id;
SELECT * FROM clients ORDER BY id;
SELECT * FROM currencys ORDER BY id;
SELECT * FROM accounts ORDER BY id;
SELECT * FROM credit ORDER BY id;
SELECT * FROM deposit ORDER BY id;
SELECT * FROM saving_account ORDER BY id;
SELECT * FROM account_operations ORDER BY id;

SELECT a.id AS account_id, a.special_number, a.account_type, a.status,
       c.id AS client_id, c.surname, c.second_name,
       cur.name AS currency
FROM accounts a
JOIN clients c ON c.id = a.client_id
JOIN currencys cur ON cur.id = a.currency_id
ORDER BY a.id;

SELECT a.id AS account_id, cr.max_credit, cr.current_dept, cr.interest_rate, cr.payment_method
FROM credit cr
JOIN accounts a ON a.id = cr.id
ORDER BY a.id;

SELECT o.id, o.account_id, o.kind, o.amount, o.description, o.created_at
FROM account_operations o
ORDER BY o.id;