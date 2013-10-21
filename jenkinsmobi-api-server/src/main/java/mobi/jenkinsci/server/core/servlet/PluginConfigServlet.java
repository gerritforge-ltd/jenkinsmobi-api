package mobi.jenkinsci.server.core.servlet;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.commons.Constants;
import mobi.jenkinsci.exceptions.TwoPhaseAuthenticationRequiredException;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginLoader;
import mobi.jenkinsci.server.realm.AccountRegistry;

import com.google.gson.Gson;
import com.google.inject.Inject;

public class PluginConfigServlet extends HttpServlet {
  private static final long serialVersionUID = -4318107949135013142L;

  @Inject
  private PluginLoader pluginLoader;

  @Inject
  AccountRegistry registry;

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    final String subscriberId = req.getHeader("x-jenkinscloud-subscriberid");
    final Account account = registry.getAccountBySubscriberId(subscriberId);
    if (account == null) {
      resp.sendError(HttpURLConnection.HTTP_NOT_FOUND, "Unknown subscriber");
      return;
    }

    final PluginConfig pluginConfig = getPluginConfig(req);
    pluginConfig.setParentAccountUsername(subscriberId);
    String reason;
    try {
      reason = getPluginValidationResult(req, account, pluginConfig);
      if (reason != null) {
        resp.sendError(HttpURLConnection.HTTP_BAD_REQUEST, reason);
      } else {
        account.addPlugin(pluginConfig);
        registry.update(account);
        resp.setStatus(HttpURLConnection.HTTP_OK);
      }
    } catch (final TwoPhaseAuthenticationRequiredException e) {
      if (e.getAuthAppId() != null) {
        resp.setHeader(Constants.X_AUTH_OTP_APP_HEADER, e.getAuthAppId());
      }
      resp.sendError(HttpURLConnection.HTTP_PRECON_FAILED,
          e.getLocalizedMessage());
    }
  }

  private String getPluginValidationResult(final HttpServletRequest req,
      final Account account, final PluginConfig pluginConfig)
      throws TwoPhaseAuthenticationRequiredException {
    final Plugin plugin =
        pluginLoader.getPlugin(pluginConfig.getKey().getType());
    return plugin.validateConfig(req, account, pluginConfig);
  }

  private PluginConfig getPluginConfig(final HttpServletRequest req)
      throws IOException {
    return new Gson().fromJson(req.getReader(), PluginConfig.class);
  }
}
