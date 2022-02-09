create table failed_event(
    event_hash text not null primary key,
    processed boolean not null default false
);

create index if not exists failed_event_processed_idx on failed_event(processed);
