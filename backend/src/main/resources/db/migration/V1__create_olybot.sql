-- Create all tables for olybot

create table if not exists accounts (
  id uuid default gen_random_uuid(),
  twitchAccountId varchar(64) default null,
  twitchName varchar(255) default null,
  primary key (id)
);

create unique index ux_username on accounts(lower(twitchAccountId));
