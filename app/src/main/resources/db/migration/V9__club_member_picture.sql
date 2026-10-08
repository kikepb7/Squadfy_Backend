-- Picture of a member in a club (specs/012-mvp-completion RN-C1); null = use the profile picture.
alter table club_service.club_members add column club_picture_url varchar(512);
