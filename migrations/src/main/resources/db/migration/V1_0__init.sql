-- Create initial storage table
create table wallet_name(
    wallet_name varchar(64) not null primary key,
    wallet_address text not null,
    created_time timestamptz not null
);

create index if not exists wallet_name_wallet_address_idx on wallet_name(wallet_address);
