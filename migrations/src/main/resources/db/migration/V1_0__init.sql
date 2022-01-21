CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create initial storage table
create table invoice(
    invoice_uuid uuid not null primary key default uuid_generate_v4(),
    data jsonb not null,
    status text not null,
    created_time timestamptz not null,
    updated_time timestamptz
);

create index if not exists invoice_status_idx on invoice(status);
