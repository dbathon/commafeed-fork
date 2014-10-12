package com.commafeed.backend.hibernate;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

public class CommafeedPostgreSQL9Dialect extends PostgreSQL9Dialect {

  public CommafeedPostgreSQL9Dialect() {
    registerColumnType(Types.CLOB, "text");

    registerFunction("pg_fts_simple_match", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN,
        "(to_tsvector('simple', ?1) @@ to_tsquery('simple', ?2))"));
  }

  @Override
  public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
    if (sqlCode == Types.CLOB) {
      return VarcharAsClobTypeDescriptor.INSTANCE;
    }
    else {
      return super.getSqlTypeDescriptorOverride(sqlCode);
    }
  }

}
