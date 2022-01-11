-- Create initial storage table
create table wallet_name(
    wallet_address varchar(64) not null primary key,
    wallet_name varchar(64) not null,
    created_time timestamptz not null
);

create index if not exists wallet_name_wallet_name_idx on wallet_name(wallet_name);
