alter table invoice add column if not exists marker_denom text not null;
alter table invoice add column if not exists marker_address text not null;
alter table invoice add column if not exists write_scope_request jsonb not null;
alter table invoice add column if not exists write_session_request jsonb not null;
alter table invoice add column if not exists write_record_request jsonb not null;
