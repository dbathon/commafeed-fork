package com.commafeed.backend.hibernate;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

public class PostgreSQL9WithTextClobDialect extends PostgreSQL9Dialect {

  public PostgreSQL9WithTextClobDialect() {
    registerColumnType(Types.CLOB, "text");
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
