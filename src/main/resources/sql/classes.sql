create table if not exists classes
(
    id bigserial not null
        constraint classes_pkey
            primary key,
    number       integer not null,
    professor_id integer not null
        constraint professor_id
            references professors,
    preqs        text,
    school_id    integer not null
        constraint school_id
            references schools,
    name         text    not null,
    role_id      bigint  not null,
    channel_id   bigint  not null,
    description  text,
    term         text    not null,
    guild_id     bigint  not null,
    identifier   text    not null,
    location     text,
    start_date   date    not null,
    end_date     date    not null,
    time         text,
    level        text
);

