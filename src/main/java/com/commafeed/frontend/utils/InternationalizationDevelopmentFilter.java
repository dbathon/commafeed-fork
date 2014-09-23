package com.commafeed.frontend.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.commafeed.backend.dao.UserSettingsDAO;
import com.commafeed.backend.model.UserSettings;
import com.commafeed.backend.services.ApplicationPropertiesService;
import com.commafeed.frontend.CommaFeedSession;

/**
 * Replace variables from templates on the fly in dev mode only. In production the substitution is
 * done at build-time.
 */
public class InternationalizationDevelopmentFilter implements Filter {

  private static Logger log = LoggerFactory.getLogger(InternationalizationDevelopmentFilter.class);

  @Inject
  UserSettingsDAO userSettingsDAO;

  private boolean production = true;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    final ApplicationPropertiesService properties = ApplicationPropertiesService.get();
    production = properties.isProduction();
  }

  @Override
  public void destroy() {
    // do nothing
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (production) {
      chain.doFilter(request, response);
      return;
    }

    final ServletOutputStream wrapper = new ServletOutputStreamWrapper();
    final ServletResponse interceptor =
        new HttpServletResponseWrapper((HttpServletResponse) response) {

          @Override
          public ServletOutputStream getOutputStream() throws IOException {
            return wrapper;
          }
        };
    chain.doFilter(request, interceptor);

    final UserSettings settings = userSettingsDAO.findByUser(CommaFeedSession.get().getUser());
    final String lang =
        (settings == null || settings.getLanguage() == null) ? "en" : settings.getLanguage();

    final byte[] bytes = translate(wrapper.toString(), lang).getBytes("UTF-8");
    response.setContentLength(bytes.length);
    response.setCharacterEncoding("UTF-8");
    response.getOutputStream().write(bytes);
    response.getOutputStream().close();

  }

  private String translate(String content, String lang) {
    final Properties props = new Properties();
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/i18n/" + lang + ".properties");
      props.load(new InputStreamReader(is, "UTF-8"));
    }
    catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    finally {
      IOUtils.closeQuietly(is);
    }

    return replace(content, props);
  }

  private static final Pattern PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

  public String replace(String content, Properties props) {
    final Matcher m = PATTERN.matcher(content);
    final StringBuffer sb = new StringBuffer();
    while (m.find()) {
      final String var = m.group(1);
      final Object replacement = props.get(var);
      final String replacementValue =
          replacement == null ? var : replacement.toString().split("#")[0];
      m.appendReplacement(sb, replacementValue);
    }
    m.appendTail(sb);
    return sb.toString();
  }

  private static class ServletOutputStreamWrapper extends ServletOutputStream {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void write(int b) throws IOException {
      baos.write(b);
    }

    @Override
    public String toString() {
      return baos.toString();
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      throw new UnsupportedOperationException();
    }

  }

}
