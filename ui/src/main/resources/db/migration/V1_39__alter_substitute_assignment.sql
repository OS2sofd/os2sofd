ALTER TABLE substitute_assignment ADD COLUMN created datetime NOT NULL DEFAULT now();
ALTER TABLE substitute_assignment ADD COLUMN  changed datetime NOT NULL DEFAULT now();
ALTER TABLE substitute_assignment_aud ADD COLUMN created datetime NULL;
ALTER TABLE substitute_assignment_aud ADD COLUMN changed datetime NULL;