INSERT INTO organisations (short_name, name, description) VALUES ('ADMORG', 'Administrativ organisation', 'Den administrative organisation');

INSERT INTO orgunit_types (id, type_key, type_value, active) VALUES (1, 'AFDELING', 'Afdeling', 1);
INSERT INTO orgunit_types (id, type_key, type_value, active) VALUES (2, 'TEAM', 'Team', 1);

INSERT INTO supported_user_types (can_order, unique_key, name, username_prefix, username_infix, username_suffix, single_user_mode) VALUES (0, 'ACTIVE_DIRECTORY', 'Active Directory', 'NONE', 'RANDOM', 'NONE', 1);
INSERT INTO supported_user_types (can_order, unique_key, name, username_prefix, username_infix, username_suffix, single_user_mode) VALUES (0, 'OPUS', 'Lønsystem', 'NONE', 'RANDOM', 'NONE', 1);
INSERT INTO supported_user_types (can_order, unique_key, name, username_prefix, username_infix, username_suffix, single_user_mode) VALUES (0, 'UNILOGIN', 'UNI-Login', 'NONE', 'RANDOM', 'NONE', 1);
INSERT INTO supported_user_types (can_order, unique_key, name, username_prefix, username_infix, username_suffix, single_user_mode) VALUES (0, 'EXCHANGE', 'Exchange', 'NONE', 'RANDOM', 'NONE', 1);
INSERT INTO supported_user_types (can_order, unique_key, name, username_prefix, username_infix, username_suffix, single_user_mode) VALUES (0, 'KSPCICS', 'KSP/CICS', 'NONE', 'RANDOM', 'NONE', 1);