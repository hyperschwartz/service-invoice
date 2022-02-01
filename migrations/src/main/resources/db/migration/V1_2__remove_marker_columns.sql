-- These columns are not necessary because a marker is not necessary to board an invoice
alter table invoice drop column if exists marker_denom;
alter table invoice drop column if exists marker_address;
