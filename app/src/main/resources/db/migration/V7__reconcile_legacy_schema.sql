-- Reconciles databases created by Hibernate (ddl-auto) with an older version of the code than the
-- V1 baseline assumed (e.g. a development database last run before the match feature): creates
-- every missing table, column and index of the current model. Idempotent: on a database that is
-- already up to date it changes nothing. NOT NULL columns without a default are added as nullable
-- so existing rows do not make it fail; unique indexes are skipped (with a notice) if data
-- violates them.

-- chat_service.chat_messages
create table if not exists chat_service.chat_messages (created_at timestamp(6) with time zone not null, chat_id uuid not null, id uuid not null, sender_id uuid not null, content varchar(255) not null, primary key (id));
alter table chat_service.chat_messages add column if not exists created_at timestamp(6) with time zone;
alter table chat_service.chat_messages add column if not exists chat_id uuid;
alter table chat_service.chat_messages add column if not exists sender_id uuid;
alter table chat_service.chat_messages add column if not exists content varchar(255);

-- chat_service.chat_participants
create table if not exists chat_service.chat_participants (created_at timestamp(6) with time zone not null, user_id uuid not null, email varchar(255) not null unique, profile_picture_url varchar(255), username varchar(255) not null unique, primary key (user_id));
alter table chat_service.chat_participants add column if not exists created_at timestamp(6) with time zone;
alter table chat_service.chat_participants add column if not exists email varchar(255);
alter table chat_service.chat_participants add column if not exists profile_picture_url varchar(255);
alter table chat_service.chat_participants add column if not exists username varchar(255);

-- chat_service.chat_participants_cross_ref
create table if not exists chat_service.chat_participants_cross_ref (chat_id uuid not null, user_id uuid not null, constraint idx_chat_participant_chat_id_user_id unique (chat_id, user_id), constraint idx_chat_participant_user_id_chat_id unique (user_id, chat_id));
alter table chat_service.chat_participants_cross_ref add column if not exists chat_id uuid;
alter table chat_service.chat_participants_cross_ref add column if not exists user_id uuid;

-- chat_service.chats
create table if not exists chat_service.chats (created_at timestamp(6) with time zone not null, creator_id uuid not null, id uuid not null, primary key (id));
alter table chat_service.chats add column if not exists created_at timestamp(6) with time zone;
alter table chat_service.chats add column if not exists creator_id uuid;

-- club_service.club_members
create table if not exists club_service.club_members (shirt_number integer, banned_at timestamp(6) with time zone, created_at timestamp(6) with time zone not null, left_at timestamp(6) with time zone, updated_at timestamp(6) with time zone not null, club_id uuid not null, id uuid not null, user_id uuid not null, position varchar(255), role varchar(255) not null check ((role in ('OWNER','ADMIN','CAPTAIN','PLAYER'))), primary key (id), constraint idx_club_members_club_user unique (club_id, user_id));
alter table club_service.club_members add column if not exists shirt_number integer;
alter table club_service.club_members add column if not exists banned_at timestamp(6) with time zone;
alter table club_service.club_members add column if not exists created_at timestamp(6) with time zone;
alter table club_service.club_members add column if not exists left_at timestamp(6) with time zone;
alter table club_service.club_members add column if not exists updated_at timestamp(6) with time zone;
alter table club_service.club_members add column if not exists club_id uuid;
alter table club_service.club_members add column if not exists user_id uuid;
alter table club_service.club_members add column if not exists position varchar(255);
alter table club_service.club_members add column if not exists role varchar(255) check ((role in ('OWNER','ADMIN','CAPTAIN','PLAYER')));

-- club_service.club_participants
create table if not exists club_service.club_participants (created_at timestamp(6) with time zone not null, user_id uuid not null, email varchar(255) not null unique, profile_picture_url varchar(255), username varchar(255) not null unique, primary key (user_id));
alter table club_service.club_participants add column if not exists created_at timestamp(6) with time zone;
alter table club_service.club_participants add column if not exists email varchar(255);
alter table club_service.club_participants add column if not exists profile_picture_url varchar(255);
alter table club_service.club_participants add column if not exists username varchar(255);

-- club_service.clubs
create table if not exists club_service.clubs (max_members integer, created_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null, id uuid not null, owner_id uuid not null, description varchar(2000), club_logo_url varchar(255), invitation_code varchar(255) not null unique, name varchar(255) not null, primary key (id));
alter table club_service.clubs add column if not exists max_members integer;
alter table club_service.clubs add column if not exists created_at timestamp(6) with time zone;
alter table club_service.clubs add column if not exists updated_at timestamp(6) with time zone;
alter table club_service.clubs add column if not exists owner_id uuid;
alter table club_service.clubs add column if not exists description varchar(2000);
alter table club_service.clubs add column if not exists club_logo_url varchar(255);
alter table club_service.clubs add column if not exists invitation_code varchar(255);
alter table club_service.clubs add column if not exists name varchar(255);

-- match_service.club_match_schedules
create table if not exists match_service.club_match_schedules (is_active boolean not null, match_duration_minutes integer not null, match_time time(0) not null, max_players integer not null, created_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null, club_id uuid not null unique, format varchar(16) not null check ((format in ('FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'))), id uuid not null, time_zone varchar(64) not null, match_day_of_week varchar(255) not null check ((match_day_of_week in ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))), primary key (id));
alter table match_service.club_match_schedules add column if not exists is_active boolean;
alter table match_service.club_match_schedules add column if not exists match_duration_minutes integer;
alter table match_service.club_match_schedules add column if not exists match_time time(0);
alter table match_service.club_match_schedules add column if not exists max_players integer;
alter table match_service.club_match_schedules add column if not exists created_at timestamp(6) with time zone;
alter table match_service.club_match_schedules add column if not exists updated_at timestamp(6) with time zone;
alter table match_service.club_match_schedules add column if not exists club_id uuid;
alter table match_service.club_match_schedules add column if not exists format varchar(16) check ((format in ('FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE')));
alter table match_service.club_match_schedules add column if not exists time_zone varchar(64);
alter table match_service.club_match_schedules add column if not exists match_day_of_week varchar(255) check ((match_day_of_week in ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')));

-- match_service.match_announcement_entries
create table if not exists match_service.match_announcement_entries (enrolled_at timestamp(6) with time zone not null, club_member_id uuid not null, id uuid not null, match_announcement_id uuid not null, status varchar(16) not null check ((status in ('CONFIRMED','WAITLISTED'))), primary key (id), constraint idx_match_announcement_entries_announcement_member unique (match_announcement_id, club_member_id));
alter table match_service.match_announcement_entries add column if not exists enrolled_at timestamp(6) with time zone;
alter table match_service.match_announcement_entries add column if not exists club_member_id uuid;
alter table match_service.match_announcement_entries add column if not exists match_announcement_id uuid;
alter table match_service.match_announcement_entries add column if not exists status varchar(16) check ((status in ('CONFIRMED','WAITLISTED')));

-- match_service.match_announcements
create table if not exists match_service.match_announcements (max_players integer not null, closes_at timestamp(6) with time zone not null, closing_reminder_sent_at timestamp(6) with time zone, created_at timestamp(6) with time zone not null, opened_notified_at timestamp(6) with time zone, opens_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null, club_id uuid not null, id uuid not null, match_id uuid not null unique, status varchar(255) not null check ((status in ('OPEN','CLOSED','CANCELLED'))), primary key (id));
alter table match_service.match_announcements add column if not exists max_players integer;
alter table match_service.match_announcements add column if not exists closes_at timestamp(6) with time zone;
alter table match_service.match_announcements add column if not exists closing_reminder_sent_at timestamp(6) with time zone;
alter table match_service.match_announcements add column if not exists created_at timestamp(6) with time zone;
alter table match_service.match_announcements add column if not exists opened_notified_at timestamp(6) with time zone;
alter table match_service.match_announcements add column if not exists opens_at timestamp(6) with time zone;
alter table match_service.match_announcements add column if not exists updated_at timestamp(6) with time zone;
alter table match_service.match_announcements add column if not exists club_id uuid;
alter table match_service.match_announcements add column if not exists match_id uuid;
alter table match_service.match_announcements add column if not exists status varchar(255) check ((status in ('OPEN','CLOSED','CANCELLED')));

-- match_service.match_events
create table if not exists match_service.match_events (minute integer, created_at timestamp(6) with time zone not null, club_member_id uuid not null, id uuid not null, match_id uuid not null, type varchar(255) not null check ((type in ('GOAL','ASSIST','YELLOW_CARD','RED_CARD'))), primary key (id));
alter table match_service.match_events add column if not exists minute integer;
alter table match_service.match_events add column if not exists created_at timestamp(6) with time zone;
alter table match_service.match_events add column if not exists club_member_id uuid;
alter table match_service.match_events add column if not exists match_id uuid;
alter table match_service.match_events add column if not exists type varchar(255) check ((type in ('GOAL','ASSIST','YELLOW_CARD','RED_CARD')));

-- match_service.match_team_players
create table if not exists match_service.match_team_players (minutes_played integer, created_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null, club_member_id uuid not null, id uuid not null, match_id uuid not null, team_side varchar(255) not null check ((team_side in ('TEAM_A','TEAM_B'))), primary key (id), constraint idx_mtp_unique_member_per_match unique (match_id, club_member_id));
alter table match_service.match_team_players add column if not exists minutes_played integer;
alter table match_service.match_team_players add column if not exists created_at timestamp(6) with time zone;
alter table match_service.match_team_players add column if not exists updated_at timestamp(6) with time zone;
alter table match_service.match_team_players add column if not exists club_member_id uuid;
alter table match_service.match_team_players add column if not exists match_id uuid;
alter table match_service.match_team_players add column if not exists team_side varchar(255) check ((team_side in ('TEAM_A','TEAM_B')));

-- match_service.matches
create table if not exists match_service.matches (duration_minutes integer not null, created_at timestamp(6) with time zone not null, scheduled_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null, club_id uuid not null, id uuid not null, status varchar(255) not null check ((status in ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED'))), primary key (id), constraint idx_matches_club_scheduled_at unique (club_id, scheduled_at));
alter table match_service.matches add column if not exists duration_minutes integer;
alter table match_service.matches add column if not exists created_at timestamp(6) with time zone;
alter table match_service.matches add column if not exists scheduled_at timestamp(6) with time zone;
alter table match_service.matches add column if not exists updated_at timestamp(6) with time zone;
alter table match_service.matches add column if not exists club_id uuid;
alter table match_service.matches add column if not exists status varchar(255) check ((status in ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')));

-- match_service.player_rating_changes
create table if not exists match_service.player_rating_changes (delta float(53) not null, created_at timestamp(6) with time zone not null, club_member_id uuid not null, id uuid not null, match_id uuid not null, primary key (id), constraint idx_player_rating_changes_match_member unique (match_id, club_member_id));
alter table match_service.player_rating_changes add column if not exists delta float(53);
alter table match_service.player_rating_changes add column if not exists created_at timestamp(6) with time zone;
alter table match_service.player_rating_changes add column if not exists club_member_id uuid;
alter table match_service.player_rating_changes add column if not exists match_id uuid;

-- match_service.player_ratings
create table if not exists match_service.player_ratings (matches_rated integer not null, rating float(53) not null, updated_at timestamp(6) with time zone not null, club_id uuid not null, club_member_id uuid not null, id uuid not null, primary key (id), constraint idx_player_ratings_club_member unique (club_id, club_member_id));
alter table match_service.player_ratings add column if not exists matches_rated integer;
alter table match_service.player_ratings add column if not exists rating float(53);
alter table match_service.player_ratings add column if not exists updated_at timestamp(6) with time zone;
alter table match_service.player_ratings add column if not exists club_id uuid;
alter table match_service.player_ratings add column if not exists club_member_id uuid;

-- notification_service.club_notification_settings
create table if not exists notification_service.club_notification_settings (muted boolean not null, updated_at timestamp(6) with time zone not null, club_id uuid not null, id uuid not null, user_id uuid not null, primary key (id), constraint idx_club_notification_settings_user_club unique (user_id, club_id));
alter table notification_service.club_notification_settings add column if not exists muted boolean;
alter table notification_service.club_notification_settings add column if not exists updated_at timestamp(6) with time zone;
alter table notification_service.club_notification_settings add column if not exists club_id uuid;
alter table notification_service.club_notification_settings add column if not exists user_id uuid;

-- notification_service.device_token
create table if not exists notification_service.device_token (created_at timestamp(6) with time zone not null, id bigint generated by default as identity, user_id uuid not null, platform varchar(255) not null check ((platform in ('ANDROID','IOS'))), token varchar(255) not null, primary key (id), constraint idx_device_tokens_token unique (token));
alter table notification_service.device_token add column if not exists created_at timestamp(6) with time zone;
alter table notification_service.device_token add column if not exists user_id uuid;
alter table notification_service.device_token add column if not exists platform varchar(255) check ((platform in ('ANDROID','IOS')));
alter table notification_service.device_token add column if not exists token varchar(255);

-- user_service.email_verification_tokens
create table if not exists user_service.email_verification_tokens (created_at timestamp(6) with time zone not null, expires_at timestamp(6) with time zone not null, id bigint generated by default as identity, used_at timestamp(6) with time zone, user_id uuid not null, token varchar(255) not null unique, primary key (id));
alter table user_service.email_verification_tokens add column if not exists created_at timestamp(6) with time zone;
alter table user_service.email_verification_tokens add column if not exists expires_at timestamp(6) with time zone;
alter table user_service.email_verification_tokens add column if not exists used_at timestamp(6) with time zone;
alter table user_service.email_verification_tokens add column if not exists user_id uuid;
alter table user_service.email_verification_tokens add column if not exists token varchar(255);

-- user_service.password_reset_tokens
create table if not exists user_service.password_reset_tokens (created_at timestamp(6) with time zone not null, expires_at timestamp(6) with time zone not null, id bigint generated by default as identity, used_at timestamp(6) with time zone, user_id uuid not null, token varchar(255) not null unique, primary key (id));
alter table user_service.password_reset_tokens add column if not exists created_at timestamp(6) with time zone;
alter table user_service.password_reset_tokens add column if not exists expires_at timestamp(6) with time zone;
alter table user_service.password_reset_tokens add column if not exists used_at timestamp(6) with time zone;
alter table user_service.password_reset_tokens add column if not exists user_id uuid;
alter table user_service.password_reset_tokens add column if not exists token varchar(255);

-- user_service.refresh_tokens
create table if not exists user_service.refresh_tokens (created_at timestamp(6) with time zone not null, expires_at timestamp(6) with time zone not null, id bigint generated by default as identity, user_id uuid not null, hashed_token varchar(255) not null, primary key (id));
alter table user_service.refresh_tokens add column if not exists created_at timestamp(6) with time zone;
alter table user_service.refresh_tokens add column if not exists expires_at timestamp(6) with time zone;
alter table user_service.refresh_tokens add column if not exists user_id uuid;
alter table user_service.refresh_tokens add column if not exists hashed_token varchar(255);

-- user_service.users
create table if not exists user_service.users (has_verified_email boolean not null, is_active boolean not null, created_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null, id uuid not null, email varchar(255) not null unique, hashed_password varchar(255) not null, username varchar(255) not null unique, primary key (id));
alter table user_service.users add column if not exists has_verified_email boolean;
alter table user_service.users add column if not exists is_active boolean;
alter table user_service.users add column if not exists created_at timestamp(6) with time zone;
alter table user_service.users add column if not exists updated_at timestamp(6) with time zone;
alter table user_service.users add column if not exists email varchar(255);
alter table user_service.users add column if not exists hashed_password varchar(255);
alter table user_service.users add column if not exists username varchar(255);

-- Indexes
create index if not exists idx_chat_message_chat_id_created_at on chat_service.chat_messages (chat_id, created_at desc);
create index if not exists idx_club_members_club_id on club_service.club_members (club_id);
create index if not exists idx_club_members_user_id on club_service.club_members (user_id);
create index if not exists idx_clubs_owner_id on club_service.clubs (owner_id);
create index if not exists idx_match_announcement_entries_announcement_id on match_service.match_announcement_entries (match_announcement_id);
create index if not exists idx_match_announcement_entries_club_member_id on match_service.match_announcement_entries (club_member_id);
create index if not exists idx_match_announcements_club_id on match_service.match_announcements (club_id);
create index if not exists idx_match_announcements_status on match_service.match_announcements (status);
create index if not exists idx_match_events_match_id on match_service.match_events (match_id);
create index if not exists idx_match_events_member_id on match_service.match_events (club_member_id);
create index if not exists idx_match_events_type on match_service.match_events (type);
create index if not exists idx_mtp_match_id on match_service.match_team_players (match_id);
create index if not exists idx_mtp_club_member_id on match_service.match_team_players (club_member_id);
create index if not exists idx_mtp_match_side on match_service.match_team_players (match_id, team_side);
create index if not exists idx_matches_club_id on match_service.matches (club_id);
create index if not exists idx_matches_scheduled_at on match_service.matches (scheduled_at);
create index if not exists idx_matches_status on match_service.matches (status);
create index if not exists idx_club_notification_settings_club_id on notification_service.club_notification_settings (club_id);
create index if not exists idx_device_tokens_user_id on notification_service.device_token (user_id);
create index if not exists idx_password_reset_token_token on user_service.email_verification_tokens (token);
create index if not exists idx_email_verification_token_token on user_service.password_reset_tokens (token);
create index if not exists idx_refresh_tokens_user_id on user_service.refresh_tokens (user_id);
create index if not exists idx_refresh_tokens_user_token on user_service.refresh_tokens (user_id, hashed_token);
create index if not exists idx_users_email on user_service.users (email);
create index if not exists idx_users_username on user_service.users (username);

-- Unique indexes (best effort: existing duplicates are reported, not fatal)
do $$
begin
    create unique index if not exists idx_chat_participant_chat_id_user_id on chat_service.chat_participants_cross_ref (chat_id, user_id);
exception when others then
    raise notice 'Skipping unique index idx_chat_participant_chat_id_user_id: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_chat_participant_user_id_chat_id on chat_service.chat_participants_cross_ref (user_id, chat_id);
exception when others then
    raise notice 'Skipping unique index idx_chat_participant_user_id_chat_id: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_club_members_club_user on club_service.club_members (club_id, user_id);
exception when others then
    raise notice 'Skipping unique index idx_club_members_club_user: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_match_announcement_entries_announcement_member on match_service.match_announcement_entries (match_announcement_id, club_member_id);
exception when others then
    raise notice 'Skipping unique index idx_match_announcement_entries_announcement_member: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_mtp_unique_member_per_match on match_service.match_team_players (match_id, club_member_id);
exception when others then
    raise notice 'Skipping unique index idx_mtp_unique_member_per_match: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_matches_club_scheduled_at on match_service.matches (club_id, scheduled_at);
exception when others then
    raise notice 'Skipping unique index idx_matches_club_scheduled_at: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_player_rating_changes_match_member on match_service.player_rating_changes (match_id, club_member_id);
exception when others then
    raise notice 'Skipping unique index idx_player_rating_changes_match_member: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_player_ratings_club_member on match_service.player_ratings (club_id, club_member_id);
exception when others then
    raise notice 'Skipping unique index idx_player_ratings_club_member: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_club_notification_settings_user_club on notification_service.club_notification_settings (user_id, club_id);
exception when others then
    raise notice 'Skipping unique index idx_club_notification_settings_user_club: %', sqlerrm;
end $$;
do $$
begin
    create unique index if not exists idx_device_tokens_token on notification_service.device_token (token);
exception when others then
    raise notice 'Skipping unique index idx_device_tokens_token: %', sqlerrm;
end $$;

-- Values for rows that existed before the match feature columns (spec 002) were added
update match_service.club_match_schedules
set format = case max_players when 10 then 'FIVE_A_SIDE' when 14 then 'SEVEN_A_SIDE' else 'ELEVEN_A_SIDE' end
where format is null;
update match_service.club_match_schedules set time_zone = 'Europe/Madrid' where time_zone is null;
update match_service.club_match_schedules set match_duration_minutes = 60 where match_duration_minutes is null;
update match_service.match_announcement_entries set status = 'CONFIRMED' where status is null;

alter table match_service.club_match_schedules
    alter column format set default 'ELEVEN_A_SIDE',
    alter column format set not null,
    alter column time_zone set default 'Europe/Madrid',
    alter column time_zone set not null,
    alter column match_duration_minutes set default 60,
    alter column match_duration_minutes set not null;

alter table match_service.match_announcement_entries
    alter column status set default 'CONFIRMED',
    alter column status set not null;
