-- create user
create user ingroup with encrypted password 'root';
grant all privileges on database efti to ingroup;
