create table email_verification_tokens
(
    id          varchar primary key,
    user_id     varchar      not null references _users (id) on delete cascade,
    token_hash  varchar(128) not null unique,
    expires_at  timestamp    not null,
    used_at     timestamp,
    created_at  timestamp default now()
);

create index idx_email_verification_tokens_user_id on email_verification_tokens (user_id);
create index idx_email_verification_tokens_expires_at on email_verification_tokens (expires_at);
