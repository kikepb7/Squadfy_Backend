-- Guests, schedule exceptions, absences, configurable close/draw and manual score (specs/008-app-parity).

-- RN-D1: close and draw deadlines of the weekly schedule (default: 1 day before at 22:00).
alter table match_service.club_match_schedules add column close_days_before integer not null default 1;
alter table match_service.club_match_schedules add column close_time time(0) not null default '22:00';
alter table match_service.club_match_schedules add column draw_days_before integer not null default 1;
alter table match_service.club_match_schedules add column draw_time time(0) not null default '22:00';

-- RN-D3: teams are published at draw_at. Existing announcements draw at their close time and the
-- closed ones are considered already published, so deploying does not redraw past matches.
alter table match_service.match_announcements add column draw_at timestamp(6) with time zone;
alter table match_service.match_announcements add column teams_published_at timestamp(6) with time zone;
update match_service.match_announcements set draw_at = closes_at;
update match_service.match_announcements set teams_published_at = closes_at where status <> 'OPEN';
alter table match_service.match_announcements alter column draw_at set not null;
create index idx_match_announcements_status_draw_at on match_service.match_announcements (status, draw_at);

-- RN-A: guests are announcement entries without club member.
alter table match_service.match_announcement_entries alter column club_member_id drop not null;
alter table match_service.match_announcement_entries add column participant_type varchar(16) not null default 'MEMBER';
alter table match_service.match_announcement_entries add column guest_name varchar(80);
alter table match_service.match_announcement_entries add column guest_position varchar(16);
alter table match_service.match_announcement_entries add column invited_by_member_id uuid;
alter table match_service.match_announcement_entries add constraint chk_match_announcement_entries_participant check (
    (participant_type = 'MEMBER' and club_member_id is not null)
    or (participant_type = 'GUEST' and club_member_id is null and guest_name is not null and invited_by_member_id is not null)
);
create index idx_match_announcement_entries_invited_by
    on match_service.match_announcement_entries (match_announcement_id, invited_by_member_id);

-- RN-A6: guests can play in a team.
alter table match_service.match_team_players alter column club_member_id drop not null;
alter table match_service.match_team_players add column guest_entry_id uuid;
alter table match_service.match_team_players add constraint chk_match_team_players_participant check (
    (club_member_id is null) <> (guest_entry_id is null)
);
create unique index idx_mtp_unique_guest_per_match on match_service.match_team_players (match_id, guest_entry_id);

-- RN-B and RN-E: week of the schedule each match belongs to and manual (official) score.
alter table match_service.matches add column schedule_date date;
alter table match_service.matches add column team_a_score integer;
alter table match_service.matches add column team_b_score integer;
create index idx_matches_club_schedule_date on match_service.matches (club_id, schedule_date);

-- Matches planned from the schedule before this version: their local date in the club's time zone.
update match_service.matches m
set schedule_date = (m.scheduled_at at time zone s.time_zone)::date
from match_service.club_match_schedules s
where s.club_id = m.club_id
  and extract(isodow from (m.scheduled_at at time zone s.time_zone)) = case s.match_day_of_week
        when 'MONDAY' then 1 when 'TUESDAY' then 2 when 'WEDNESDAY' then 3 when 'THURSDAY' then 4
        when 'FRIDAY' then 5 when 'SATURDAY' then 6 else 7 end;

-- RN-B: cancelled or moved weeks of the schedule.
create table match_service.schedule_exceptions (
    id uuid not null,
    club_id uuid not null,
    schedule_date date not null,
    type varchar(16) not null,
    new_scheduled_at timestamp(6) with time zone,
    reason varchar(200),
    affected_match_id uuid,
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint idx_schedule_exceptions_club_date unique (club_id, schedule_date),
    constraint chk_schedule_exceptions_type check (
        (type = 'CANCELLED' and new_scheduled_at is null) or (type = 'RESCHEDULED' and new_scheduled_at is not null)
    )
);

-- RN-C: absences of members.
create table match_service.member_absences (
    id uuid not null,
    club_id uuid not null,
    club_member_id uuid not null,
    from_date date not null,
    to_date date not null,
    reason varchar(200),
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint chk_member_absences_dates check (from_date <= to_date)
);

create index idx_member_absences_club_member on match_service.member_absences (club_id, club_member_id);
create index idx_member_absences_club_dates on match_service.member_absences (club_id, from_date, to_date);
