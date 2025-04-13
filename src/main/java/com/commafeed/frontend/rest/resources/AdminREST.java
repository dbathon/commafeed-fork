package com.commafeed.frontend.rest.resources;

import com.commafeed.backend.MetricsBean;
import com.commafeed.backend.StartupBean;
import com.commafeed.backend.dao.UserDAO;
import com.commafeed.backend.dao.UserRoleDAO;
import com.commafeed.backend.feeds.FeedRefreshTaskGiver;
import com.commafeed.backend.feeds.FeedRefreshUpdater;
import com.commafeed.backend.feeds.FeedRefreshWorker;
import com.commafeed.backend.model.ApplicationSettings;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserRole;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.services.PasswordEncryptionService;
import com.commafeed.backend.services.UserService;
import com.commafeed.frontend.model.UserModel;
import com.commafeed.frontend.model.request.IDRequest;
import com.commafeed.frontend.rest.RestSecurityCheck;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang.StringUtils;

@RestSecurityCheck(Role.ADMIN)
@Path("/admin")
@Api(value = "/admin", description = "Operations about application administration")
public class AdminREST extends AbstractREST {

  @Inject
  private UserService userService;

  @Inject
  private UserDAO userDAO;

  @Inject
  private UserRoleDAO userRoleDAO;

  @Inject
  private MetricsBean metricsBean;

  @Inject
  private FeedRefreshWorker feedRefreshWorker;

  @Inject
  private FeedRefreshUpdater feedRefreshUpdater;

  @Inject
  private FeedRefreshTaskGiver taskGiver;

  @Inject
  private PasswordEncryptionService encryptionService;

  @Path("/user/save")
  @POST
  @ApiOperation(value = "Save or update a user",
      notes = "Save or update a user. If the id is not specified, a new user will be created")
  public Response save(@ApiParam(required = true) UserModel userModel) {
    Preconditions.checkNotNull(userModel);
    Preconditions.checkNotNull(userModel.getName());

    final Long id = userModel.getId();
    if (id == null) {
      Preconditions.checkNotNull(userModel.getPassword());

      final Set<Role> roles = Sets.newHashSet(Role.USER);
      if (userModel.isAdmin()) {
        roles.add(Role.ADMIN);
      }
      try {
        userService.register(userModel.getName(), userModel.getPassword(), userModel.getEmail(),
            roles, true);
      } catch (final Exception e) {
        return Response.status(Status.CONFLICT).entity(e.getMessage()).build();
      }
    } else {
      final User user = userDAO.findById(id);
      if (StartupBean.USERNAME_ADMIN.equals(user.getName()) && !userModel.isEnabled()) {
        return Response.status(Status.FORBIDDEN).entity("You cannot disable the admin user.")
            .build();
      }
      user.setName(userModel.getName());
      if (StringUtils.isNotBlank(userModel.getPassword())) {
        user.setPassword(encryptionService.getEncryptedPassword(userModel.getPassword(),
            user.getSalt()));
      }
      user.setEmail(userModel.getEmail());
      user.setDisabled(!userModel.isEnabled());
      userDAO.saveOrUpdate(user);

      final Set<Role> roles = userRoleDAO.findRoles(user);
      if (userModel.isAdmin() && !roles.contains(Role.ADMIN)) {
        userRoleDAO.saveOrUpdate(new UserRole(user, Role.ADMIN));
      } else if (!userModel.isAdmin() && roles.contains(Role.ADMIN)) {
        if (StartupBean.USERNAME_ADMIN.equals(user.getName())) {
          return Response.status(Status.FORBIDDEN)
              .entity("You cannot remove the admin role from the admin user.").build();
        }
        for (final UserRole userRole : userRoleDAO.findAll(user)) {
          if (userRole.getRole() == Role.ADMIN) {
            userRoleDAO.delete(userRole);
          }
        }
      }

    }
    return Response.ok(Status.OK).entity("OK").build();

  }

  @Path("/user/get/{id}")
  @GET
  @ApiOperation(value = "Get user information", notes = "Get user information",
      responseClass = "com.commafeed.frontend.model.UserModel")
  public Response getUser(@ApiParam(value = "user id", required = true) @PathParam("id") Long id) {
    Preconditions.checkNotNull(id);
    final User user = userDAO.findById(id);
    final UserModel userModel = new UserModel();
    userModel.setId(user.getId());
    userModel.setName(user.getName());
    userModel.setEmail(user.getEmail());
    userModel.setEnabled(!user.isDisabled());
    for (final UserRole role : userRoleDAO.findAll(user)) {
      if (role.getRole() == Role.ADMIN) {
        userModel.setAdmin(true);
      }
    }
    return Response.ok(userModel).build();
  }

  @Path("/user/getAll")
  @GET
  @ApiOperation(value = "Get all users", notes = "Get all users",
      responseClass = "List[com.commafeed.frontend.model.UserModel]")
  public Response getUsers() {
    final Map<Long, UserModel> users = Maps.newHashMap();
    for (final UserRole role : userRoleDAO.findAll()) {
      final User user = role.getUser();
      final Long key = user.getId();
      UserModel userModel = users.get(key);
      if (userModel == null) {
        userModel = new UserModel();
        userModel.setId(user.getId());
        userModel.setName(user.getName());
        userModel.setEmail(user.getEmail());
        userModel.setEnabled(!user.isDisabled());
        userModel.setCreated(user.getCreated());
        userModel.setLastLogin(user.getLastLogin());
        users.put(key, userModel);
      }
      if (role.getRole() == Role.ADMIN) {
        userModel.setAdmin(true);
      }
    }
    return Response.ok(users.values()).build();
  }

  @Path("/user/delete")
  @POST
  @ApiOperation(value = "Delete a user", notes = "Delete a user, and all his subscriptions")
  public Response delete(@ApiParam(required = true) IDRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final User user = userDAO.findById(req.getId());
    if (user == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    if (StartupBean.USERNAME_ADMIN.equals(user.getName())) {
      return Response.status(Status.FORBIDDEN).entity("You cannot delete the admin user.").build();
    }
    userService.unregister(user);
    return Response.ok().build();
  }

  @Path("/settings")
  @GET
  @ApiOperation(value = "Retrieve application settings", notes = "Retrieve application settings",
      responseClass = "com.commafeed.backend.model.ApplicationSettings")
  public Response getSettings() {
    return Response.ok(applicationSettingsService.get()).build();
  }

  @Path("/settings")
  @POST
  @ApiOperation(value = "Save application settings", notes = "Save application settings")
  public Response saveSettings(@ApiParam(required = true) ApplicationSettings settings) {
    Preconditions.checkNotNull(settings);
    applicationSettingsService.save(settings);
    return Response.ok().build();
  }

  @Path("/metrics")
  @GET
  @ApiOperation(value = "Retrieve server metrics")
  public Response getMetrics(@QueryParam("backlog") @DefaultValue("false") boolean backlog) {
    final Map<String, Object> map = Maps.newLinkedHashMap();
    map.put("lastMinute", metricsBean.getLastMinute());
    map.put("lastHour", metricsBean.getLastHour());
    if (backlog) {
      map.put("backlog", taskGiver.getUpdatableCount());
    }
    map.put("http_active", feedRefreshWorker.getActiveCount());
    map.put("http_queue", feedRefreshWorker.getQueueSize());
    map.put("database_active", feedRefreshUpdater.getActiveCount());
    map.put("database_queue", feedRefreshUpdater.getQueueSize());
    map.put("cache", metricsBean.getCacheStats());

    return Response.ok(map).build();
  }

}
