-- Align table and column names with the domain (MatchAnnouncement) and drop the column
-- replaced by the "open the day after the previous match" rule (specs/002-match-cycle).

alter table match_service.callups rename to match_announcements;
alter index if exists match_service.idx_callups_club_id rename to idx_match_announcements_club_id;
alter index if exists match_service.idx_callups_status rename to idx_match_announcements_status;
alter index if exists match_service.idx_callups_match_id rename to idx_match_announcements_match_id;

alter table match_service.callup_entries rename to match_announcement_entries;
alter table match_service.match_announcement_entries rename column callup_id to match_announcement_id;
alter index if exists match_service.idx_callup_entries_callup_id rename to idx_match_announcement_entries_announcement_id;
alter index if exists match_service.idx_callup_entries_club_member_id rename to idx_match_announcement_entries_club_member_id;
alter index if exists match_service.idx_callup_entries_callup_member rename to idx_match_announcement_entries_announcement_member;

alter table match_service.club_match_schedules drop column if exists callup_open_days_before_match;
