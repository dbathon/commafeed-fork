package com.commafeed.backend.model;

import java.io.Serializable;
import javax.persistence.*;

@SuppressWarnings("serial")
@MappedSuperclass
public abstract class AbstractModel implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "gen")
  @TableGenerator(name = "gen", table = "hibernate_sequences", pkColumnName = "sequence_name",
      valueColumnName = "sequence_next_hi_value", allocationSize = 1000)
  private Long id;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

}
