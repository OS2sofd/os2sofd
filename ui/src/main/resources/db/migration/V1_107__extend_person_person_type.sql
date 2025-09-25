ALTER TABLE persons ADD person_type varchar(100) DEFAULT 'PERSON' NOT NULL;
ALTER TABLE persons_aud ADD person_type varchar(100) NULL;