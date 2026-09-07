create table stock (
    sku                 varchar(64) primary key,
    warehouse_id        varchar(64) not null,
    quantity_on_hand    integer not null,
    quantity_reserved   integer not null default 0
);

create sequence outbox_messages_seq start with 1 increment by 50;

create table outbox_messages (
    id              uuid primary key,
    topic           varchar(100) not null,
    message_key     varchar(100) not null,
    event_type      varchar(100) not null,
    payload         text not null,
    occurred_at     timestamptz not null,
    dispatched_at   timestamptz
);

create index ix_outbox_messages_pending
    on outbox_messages (occurred_at)
    where dispatched_at is null;

create table processed_events (
    event_id        uuid primary key,
    processed_at    timestamptz not null
);
