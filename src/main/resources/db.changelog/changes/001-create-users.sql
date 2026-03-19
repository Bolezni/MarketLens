create table _users{
    id VARCHAR primary key,
    email VARCHAR(255) unique not null,
    password varchar(255) not null,
    plan varchar(255),
    created_at TIMESTAMP DEFAULT NOW()
}