create table payment(
    payment_uuid uuid not null primary key default uuid_generate_v4(),
    invoice_uuid uuid not null,
    payment_time timestamptz not null,
    from_address text not null,
    to_address text not null,
    payment_amount numeric(1000, 15) not null,
    created_time timestamptz not null,
    updated_time timestamptz,
    constraint fk_invoice foreign key(invoice_uuid) references invoice(invoice_uuid)
);

create index if not exists payment_invoice_uuid_idx on payment(invoice_uuid);
create index if not exists payment_from_address_idx on payment(from_address);
create index if not exists payment_to_address_idx on payment(to_address);
