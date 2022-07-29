create table account
(
    id          uuid primary key default gen_random_uuid(),
    customer_id uuid not null,
    country     text not null
);

create index idx_account_id on account (id);

create table account_balance
(
    account_id uuid references account,
    currency   text    not null,
    balance    numeric not null default 0
);

create index idx_account_balance_account_id on account_balance (account_id);

create table transaction
(
    id          uuid primary key         default gen_random_uuid(),
    account_id  uuid references account,
    amount      numeric                                not null,
    currency    text                                   not null,
    direction   text                                   not null,
    description text                                   not null,
    created_at  timestamp with time zone default now() not null
);

create index idx_transaction_account_id on transaction (account_id);
create index idx_transaction_id on transaction (id);
