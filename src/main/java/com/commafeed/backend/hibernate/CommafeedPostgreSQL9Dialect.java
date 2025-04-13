package com.commafeed.backend.hibernate;

import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class CommafeedPostgreSQL9Dialect extends PostgreSQL9Dialect {

  public CommafeedPostgreSQL9Dialect() {
    registerFunction("pg_fts_simple_match", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN,
        "(to_tsvector('simple', ?1) @@ to_tsquery('simple', ?2))"));
  }

}
