package com.commafeed.backend.services;

import com.commafeed.backend.dao.FeedCategoryDAO;
import com.commafeed.backend.dao.UserDAO;
import com.commafeed.backend.dao.UserSettingsDAO;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserRole;
import com.commafeed.backend.model.UserRole.Role;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

@Stateless
public class UserService {

  @Inject
  private UserDAO userDAO;

  @Inject
  private FeedCategoryDAO feedCategoryDAO;

  @Inject
  private UserSettingsDAO userSettingsDAO;

  @Inject
  private PasswordEncryptionService encryptionService;

  @Inject
  private ApplicationSettingsService applicationSettingsService;

  public User login(String name, String password) {
    if (name == null || password == null) {
      return null;
    }

    final User user = userDAO.findByName(name);
    if (user != null && !user.isDisabled()) {
      final boolean authenticated =
          encryptionService.authenticate(password, user.getPassword(), user.getSalt());
      if (authenticated) {
        final Date lastLogin = user.getLastLogin();
        final Date now = new Date();
        // only update lastLogin field every hour in order to not
        // invalidate the cache everytime someone logs in
        if (lastLogin == null || lastLogin.before(DateUtils.addHours(now, -1))) {
          user.setLastLogin(now);
          userDAO.saveOrUpdate(user);
        }
        return user;
      }
    }
    return null;
  }

  public User register(String name, String password, String email, Collection<Role> roles) {
    return register(name, password, email, roles, false);
  }

  public User register(String name, String password, String email, Collection<Role> roles,
                       boolean forceRegistration) {

    Preconditions.checkNotNull(name);
    Preconditions.checkArgument(StringUtils.length(name) <= 32,
        "Name too long (32 characters maximum)");
    Preconditions.checkNotNull(password);

    if (!forceRegistration) {
      Preconditions.checkState(applicationSettingsService.get().isAllowRegistrations(),
          "Registrations are closed on this CommaFeed instance");

      Preconditions.checkNotNull(email);
      Preconditions.checkArgument(StringUtils.length(name) >= 3,
          "Name too short (3 characters minimum)");
      Preconditions.checkArgument(forceRegistration || StringUtils.length(password) >= 6,
          "Password too short (6 characters maximum)");
      Preconditions.checkArgument(StringUtils.contains(email, "@"), "Invalid email address");
    }

    Preconditions.checkArgument(userDAO.findByName(name) == null, "Name already taken");
    if (StringUtils.isNotBlank(email)) {
      Preconditions.checkArgument(userDAO.findByEmail(email) == null, "Email already taken");
    }

    final User user = new User();
    final byte[] salt = encryptionService.generateSalt();
    user.setName(name);
    user.setEmail(email);
    user.setCreated(new Date());
    user.setSalt(salt);
    user.setPassword(encryptionService.getEncryptedPassword(password, salt));
    for (final Role role : roles) {
      user.getRoles().add(new UserRole(user, role));
    }
    userDAO.saveOrUpdate(user);
    return user;
  }

  public void unregister(User user) {
    feedCategoryDAO.delete(feedCategoryDAO.findAll(user));
    userSettingsDAO.delete(userSettingsDAO.findByUser(user));
    userDAO.delete(user);
  }

  public String generateApiKey(User user) {
    final byte[] key =
        encryptionService.getEncryptedPassword(UUID.randomUUID().toString(), user.getSalt());
    return DigestUtils.sha1Hex(key);
  }
}
