ALTER TABLE posts ADD COLUMN return_address TINYINT(1) NOT NULL;
ALTER TABLE posts_aud ADD COLUMN return_address TINYINT(1) NULL;

UPDATE posts SET return_address = prime WHERE (id in (SELECT post_id FROM orgunits_posts));