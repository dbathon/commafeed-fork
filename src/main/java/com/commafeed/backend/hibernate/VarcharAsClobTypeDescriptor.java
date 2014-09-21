package com.commafeed.backend.hibernate;

import java.sql.Types;

import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

public class VarcharAsClobTypeDescriptor extends VarcharTypeDescriptor {

  public static final VarcharAsClobTypeDescriptor INSTANCE = new VarcharAsClobTypeDescriptor();

  @Override
  public int getSqlType() {
    return Types.CLOB;
  }

}
