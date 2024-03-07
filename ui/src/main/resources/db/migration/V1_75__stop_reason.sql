ALTER TABLE persons ADD COLUMN stop_reason TEXT NULL AFTER force_stop;

-- cleanup unused fields on _aud table
ALTER TABLE persons_aud DROP COLUMN last_changed;
ALTER TABLE persons_aud DROP COLUMN key_words;
ALTER TABLE persons_aud DROP COLUMN notes;
