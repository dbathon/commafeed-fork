package com.commafeed.backend.model;

import javax.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "USERROLES")
@SuppressWarnings("serial")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class UserRole extends AbstractModel {

  public static enum Role {
    USER,
    ADMIN,
    NONE
  }

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "roleName", nullable = false)
  @Enumerated(EnumType.STRING)
  private Role role;

  public UserRole() {

  }

  public UserRole(User user, Role role) {
    this.user = user;
    this.role = role;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

}
