-- Minutes played and derived statistics (specs/004-match-results-stats).

-- RN-6: match duration (copied from the club schedule) and per-player minutes override.
alter table match_service.club_match_schedules add column match_duration_minutes integer not null default 60;
alter table match_service.matches add column duration_minutes integer not null default 60;
alter table match_service.match_team_players add column minutes_played integer;

-- RN-7: statistics are computed from completed matches; these counters were never updated.
alter table club_service.club_members
    drop column goals,
    drop column assists,
    drop column yellow_cards,
    drop column red_cards,
    drop column minutes_played,
    drop column matches_played;
