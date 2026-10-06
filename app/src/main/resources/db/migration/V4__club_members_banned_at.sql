-- Reversible ban of a club member (specs/001-clubs-membership RN-14). A banned membership cannot
-- be reactivated through an invitation code until a manager lifts the ban (banned_at back to null).

alter table club_service.club_members add column banned_at timestamp(6) with time zone;
