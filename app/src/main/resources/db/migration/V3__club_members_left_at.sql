-- Soft delete of club memberships (specs/001-clubs-membership RN-10, RN-12): members who leave or
-- are removed keep their row (history, ratings and match events reference club_member_id).
-- An active membership has left_at IS NULL; rejoining clears it again.

alter table club_service.club_members add column left_at timestamp(6) with time zone;
