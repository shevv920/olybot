-- add settings to enable/disable bot for account's channel

alter table accounts
	add column botEnabled boolean default false,
	add column botApproved boolean default false
;
