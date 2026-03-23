create table password_reset_tokens
(
    id          varchar primary key,
    user_id     varchar      not null references _users (id) on delete cascade,
    token_hash  varchar(128) not null unique,
    expires_at timestamp    not null,
    used_at    timestamp,
    created_at timestamp default now()
);

create index idx_password_reset_tokens_user_id on password_reset_tokens (user_id);
create index idx_password_reset_tokens_expires_at on password_reset_tokens (expires_at);

