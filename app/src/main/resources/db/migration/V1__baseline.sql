-- Baseline schema of every module, matching the database that Hibernate (ddl-auto: update)
-- created before Flyway was introduced. Existing databases are baselined at this version
-- (spring.flyway.baseline-on-migrate) and only run the migrations that follow.

create schema if not exists user_service;
create schema if not exists chat_service;
create schema if not exists notification_service;
create schema if not exists club_service;
create schema if not exists match_service;

create table chat_service.chat_messages (
    created_at timestamp(6) with time zone not null,
    chat_id uuid not null,
    id uuid not null,
    sender_id uuid not null,
    content varchar(255) not null,
    primary key (id)
);

create table chat_service.chat_participants (
    created_at timestamp(6) with time zone not null,
    user_id uuid not null,
    email varchar(255) not null unique,
    profile_picture_url varchar(255),
    username varchar(255) not null unique,
    primary key (user_id)
);

create table chat_service.chat_participants_cross_ref (
    chat_id uuid not null,
    user_id uuid not null,
    constraint idx_chat_participant_chat_id_user_id unique (chat_id, user_id),
    constraint idx_chat_participant_user_id_chat_id unique (user_id, chat_id)
);

create table chat_service.chats (
    created_at timestamp(6) with time zone not null,
    creator_id uuid not null,
    id uuid not null,
    primary key (id)
);

create index idx_chat_message_chat_id_created_at 
   on chat_service.chat_messages (chat_id, created_at desc);

create table club_service.club_members (
    assists integer not null,
    goals integer not null,
    matches_played integer not null,
    minutes_played integer not null,
    red_cards integer not null,
    shirt_number integer,
    yellow_cards integer not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    club_id uuid not null,
    id uuid not null,
    user_id uuid not null,
    position varchar(255),
    role varchar(255) not null check ((role in ('OWNER','ADMIN','CAPTAIN','PLAYER'))),
    primary key (id),
    constraint idx_club_members_club_user unique (club_id, user_id)
);

create table club_service.club_participants (
    created_at timestamp(6) with time zone not null,
    user_id uuid not null,
    email varchar(255) not null unique,
    profile_picture_url varchar(255),
    username varchar(255) not null unique,
    primary key (user_id)
);

create table club_service.clubs (
    max_members integer,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    id uuid not null,
    owner_id uuid not null,
    description varchar(2000),
    club_logo_url varchar(255),
    invitation_code varchar(255) not null unique,
    name varchar(255) not null,
    primary key (id)
);

create index idx_club_members_club_id 
   on club_service.club_members (club_id);

create index idx_club_members_user_id 
   on club_service.club_members (user_id);

create index idx_clubs_owner_id 
   on club_service.clubs (owner_id);

create table match_service.callup_entries (
    enrolled_at timestamp(6) with time zone not null,
    callup_id uuid not null,
    club_member_id uuid not null,
    id uuid not null,
    status varchar(16) not null default 'CONFIRMED' check ((status in ('CONFIRMED','WAITLISTED'))),
    primary key (id),
    constraint idx_callup_entries_callup_member unique (callup_id, club_member_id)
);

create table match_service.callups (
    max_players integer not null,
    closes_at timestamp(6) with time zone not null,
    created_at timestamp(6) with time zone not null,
    opens_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    club_id uuid not null,
    id uuid not null,
    match_id uuid not null unique,
    status varchar(255) not null check ((status in ('OPEN','CLOSED','CANCELLED'))),
    primary key (id)
);

create table match_service.club_match_schedules (
    is_active boolean not null,
    match_time time(0) not null,
    max_players integer not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    club_id uuid not null unique,
    id uuid not null,
    format varchar(16) not null default 'ELEVEN_A_SIDE' check ((format in ('FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'))),
    match_day_of_week varchar(255) not null check ((match_day_of_week in ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))),
    time_zone varchar(64) not null default 'Europe/Madrid',
    primary key (id)
);

create table match_service.match_events (
    minute integer,
    created_at timestamp(6) with time zone not null,
    club_member_id uuid not null,
    id uuid not null,
    match_id uuid not null,
    type varchar(255) not null check ((type in ('GOAL','ASSIST','YELLOW_CARD','RED_CARD'))),
    primary key (id)
);

create table match_service.match_team_players (
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    club_member_id uuid not null,
    id uuid not null,
    match_id uuid not null,
    team_side varchar(255) not null check ((team_side in ('TEAM_A','TEAM_B'))),
    primary key (id),
    constraint idx_mtp_unique_member_per_match unique (match_id, club_member_id)
);

create table match_service.matches (
    created_at timestamp(6) with time zone not null,
    scheduled_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    club_id uuid not null,
    id uuid not null,
    status varchar(255) not null check ((status in ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED'))),
    primary key (id),
    constraint idx_matches_club_scheduled_at unique (club_id, scheduled_at)
);

create table match_service.player_rating_changes (
    delta float(53) not null,
    created_at timestamp(6) with time zone not null,
    club_member_id uuid not null,
    id uuid not null,
    match_id uuid not null,
    primary key (id),
    constraint idx_player_rating_changes_match_member unique (match_id, club_member_id)
);

create table match_service.player_ratings (
    matches_rated integer not null,
    rating float(53) not null,
    updated_at timestamp(6) with time zone not null,
    club_id uuid not null,
    club_member_id uuid not null,
    id uuid not null,
    primary key (id),
    constraint idx_player_ratings_club_member unique (club_id, club_member_id)
);

create index idx_callup_entries_callup_id 
   on match_service.callup_entries (callup_id);

create index idx_callup_entries_club_member_id 
   on match_service.callup_entries (club_member_id);

create index idx_callups_club_id 
   on match_service.callups (club_id);

create index idx_callups_status 
   on match_service.callups (status);

create index idx_match_events_match_id 
   on match_service.match_events (match_id);

create index idx_match_events_member_id 
   on match_service.match_events (club_member_id);

create index idx_match_events_type 
   on match_service.match_events (type);

create index idx_mtp_match_id 
   on match_service.match_team_players (match_id);

create index idx_mtp_club_member_id 
   on match_service.match_team_players (club_member_id);

create index idx_mtp_match_side 
   on match_service.match_team_players (match_id, team_side);

create index idx_matches_club_id 
   on match_service.matches (club_id);

create index idx_matches_scheduled_at 
   on match_service.matches (scheduled_at);

create index idx_matches_status 
   on match_service.matches (status);

create table notification_service.device_token (
    created_at timestamp(6) with time zone not null,
    id bigint generated by default as identity,
    user_id uuid not null,
    platform varchar(255) not null check ((platform in ('ANDROID','IOS'))),
    token varchar(255) not null,
    primary key (id),
    constraint idx_device_tokens_token unique (token)
);

create index idx_device_tokens_user_id 
   on notification_service.device_token (user_id);

create table user_service.email_verification_tokens (
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    id bigint generated by default as identity,
    used_at timestamp(6) with time zone,
    user_id uuid not null,
    token varchar(255) not null unique,
    primary key (id)
);

create table user_service.password_reset_tokens (
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    id bigint generated by default as identity,
    used_at timestamp(6) with time zone,
    user_id uuid not null,
    token varchar(255) not null unique,
    primary key (id)
);

create table user_service.refresh_tokens (
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    id bigint generated by default as identity,
    user_id uuid not null,
    hashed_token varchar(255) not null,
    primary key (id)
);

create table user_service.users (
    has_verified_email boolean not null,
    is_active boolean not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    id uuid not null,
    email varchar(255) not null unique,
    hashed_password varchar(255) not null,
    username varchar(255) not null unique,
    primary key (id)
);

create index idx_password_reset_token_token 
   on user_service.email_verification_tokens (token);

create index idx_email_verification_token_token 
   on user_service.password_reset_tokens (token);

create index idx_refresh_tokens_user_id 
   on user_service.refresh_tokens (user_id);

create index idx_refresh_tokens_user_token 
   on user_service.refresh_tokens (user_id, hashed_token);

create index idx_users_email 
   on user_service.users (email);

create index idx_users_username 
   on user_service.users (username);

alter table if exists chat_service.chat_messages 
   add constraint FKt56nsqjwt7t4sian6vts9wg3t 
   foreign key (chat_id) 
   references chat_service.chats 
   on delete cascade;

alter table if exists chat_service.chat_messages 
   add constraint FKra53n2lu9wk61w5j7ovxo4as7 
   foreign key (sender_id) 
   references chat_service.chat_participants;

alter table if exists chat_service.chat_participants_cross_ref 
   add constraint FKldco6scbjpnbwsf67ului3git 
   foreign key (user_id) 
   references chat_service.chat_participants;

alter table if exists chat_service.chat_participants_cross_ref 
   add constraint FKmgbllw57tuabb1cqsl9uacrjc 
   foreign key (chat_id) 
   references chat_service.chats;

alter table if exists chat_service.chats 
   add constraint FKny26by9vuy9u6f90rxq42ul6s 
   foreign key (creator_id) 
   references chat_service.chat_participants;

alter table if exists club_service.club_members 
   add constraint FKsra64bwv0uy24tsb8aade6vya 
   foreign key (user_id) 
   references club_service.club_participants;

alter table if exists user_service.email_verification_tokens 
   add constraint FKi1c4mmamlb8keqt74k4lrtwhc 
   foreign key (user_id) 
   references user_service.users;

alter table if exists user_service.password_reset_tokens 
   add constraint FKk3ndxg5xp6v7wd4gjyusp15gq 
   foreign key (user_id) 
   references user_service.users;
