-- Match cycle push notifications (specs/005-match-notifications).

-- RN-5: opening and closing reminder are sent at most once per announcement.
alter table match_service.match_announcements add column opened_notified_at timestamp(6) with time zone;
alter table match_service.match_announcements add column closing_reminder_sent_at timestamp(6) with time zone;

-- Announcements that already existed are considered notified, so deploying does not send a burst
-- of late notifications.
update match_service.match_announcements
set opened_notified_at = now(), closing_reminder_sent_at = now();

-- RN-7: per user and club mute preference.
create table notification_service.club_notification_settings (
    id uuid not null,
    user_id uuid not null,
    club_id uuid not null,
    muted boolean not null,
    updated_at timestamp(6) with time zone not null,
    primary key (id),
    constraint idx_club_notification_settings_user_club unique (user_id, club_id)
);

create index idx_club_notification_settings_club_id on notification_service.club_notification_settings (club_id);
