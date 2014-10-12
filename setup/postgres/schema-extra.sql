create index idx_feedentrycontents_fts on FEEDENTRYCONTENTS using gin(to_tsvector('simple', searchText));
