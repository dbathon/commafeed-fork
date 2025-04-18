package com.commafeed.backend;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpGetter {

  private static Logger log = LoggerFactory.getLogger(HttpGetter.class);

  private static final String USER_AGENT = "CommaFeed/1.0 (http://www.commafeed.com)";
  private static final String ACCEPT_LANGUAGE = "en";
  private static final String PRAGMA_NO_CACHE = "No-cache";
  private static final String CACHE_CONTROL_NO_CACHE = "no-cache";
  private static final String UTF8 = "UTF-8";
  private static final String HTTPS = "https";

  private static SSLContext SSL_CONTEXT = null;

  static {
    try {
      SSL_CONTEXT = SSLContext.getInstance("TLS");
      SSL_CONTEXT.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()},
          new SecureRandom());
    } catch (final Exception e) {
      log.error("Could not configure ssl context");
    }
  }

  private static final X509HostnameVerifier VERIFIER = new DefaultHostnameVerifier();

  public HttpResult getBinary(String url, int timeout) throws ClientProtocolException, IOException,
      NotModifiedException {
    return getBinary(url, null, null, timeout);
  }

  /**
   * @param url          the url to retrive
   * @param lastModified header we got last time we queried that url, or null
   * @param eTag         header we got last time we queried that url, or null
   * @return
   * @throws ClientProtocolException
   * @throws IOException
   * @throws NotModifiedException    if the url hasn't changed since we asked for it last time
   */
  public HttpResult getBinary(String url, String lastModified, String eTag, int timeout)
      throws ClientProtocolException, IOException, NotModifiedException {
    HttpResult result = null;
    final long start = System.currentTimeMillis();

    final HttpClient client = newClient(timeout);
    try {
      final HttpGet httpget = new HttpGet(url);
      httpget.addHeader(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
      httpget.addHeader(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE);
      httpget.addHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_CACHE);
      httpget.addHeader(HttpHeaders.USER_AGENT, USER_AGENT);

      if (lastModified != null) {
        httpget.addHeader(HttpHeaders.IF_MODIFIED_SINCE, lastModified);
      }
      if (eTag != null) {
        httpget.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
      }

      HttpResponse response = null;
      try {
        response = client.execute(httpget);
        final int code = response.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_NOT_MODIFIED) {
          throw new NotModifiedException("304 http code");
        } else if (code >= 300) {
          throw new HttpResponseException(code, "Server returned HTTP error code " + code);
        }

      } catch (final HttpResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
          throw new NotModifiedException("304 http code");
        } else {
          throw e;
        }
      }
      final Header lastModifiedHeader = response.getFirstHeader(HttpHeaders.LAST_MODIFIED);
      final Header eTagHeader = response.getFirstHeader(HttpHeaders.ETAG);

      final String lastModifiedResponse =
          lastModifiedHeader == null ? null : StringUtils.trimToNull(lastModifiedHeader.getValue());
      if (lastModified != null && StringUtils.equals(lastModified, lastModifiedResponse)) {
        throw new NotModifiedException("lastModifiedHeader is the same");
      }

      final String eTagResponse =
          eTagHeader == null ? null : StringUtils.trimToNull(eTagHeader.getValue());
      if (eTag != null && StringUtils.equals(eTag, eTagResponse)) {
        throw new NotModifiedException("eTagHeader is the same");
      }

      final HttpEntity entity = response.getEntity();
      byte[] content = null;
      Header contentType = null;
      if (entity != null) {
        content = EntityUtils.toByteArray(entity);
        contentType = entity.getContentType();
      }

      final long duration = System.currentTimeMillis() - start;
      result =
          new HttpResult(content, contentType == null ? null : contentType.getValue(),
              lastModifiedHeader == null ? null : lastModifiedHeader.getValue(), eTagHeader == null
              ? null : eTagHeader.getValue(), duration);
    } finally {
      client.getConnectionManager().shutdown();
    }
    return result;
  }

  public static class HttpResult {

    private final byte[] content;
    private final String contentType;
    private final String lastModifiedSince;
    private final String eTag;
    private final long duration;

    public HttpResult(byte[] content, String contentType, String lastModifiedSince, String eTag,
                      long duration) {
      this.content = content;
      this.contentType = contentType;
      this.lastModifiedSince = lastModifiedSince;
      this.eTag = eTag;
      this.duration = duration;
    }

    public byte[] getContent() {
      return content;
    }

    public String getContentType() {
      return contentType;
    }

    public String getLastModifiedSince() {
      return lastModifiedSince;
    }

    public String geteTag() {
      return eTag;
    }

    public long getDuration() {
      return duration;
    }

  }

  public static HttpClient newClient(int timeout) {
    final DefaultHttpClient client = new SystemDefaultHttpClient();

    final SSLSocketFactory ssf = new SSLSocketFactory(SSL_CONTEXT, VERIFIER);
    final ClientConnectionManager ccm = client.getConnectionManager();
    final SchemeRegistry sr = ccm.getSchemeRegistry();
    sr.register(new Scheme(HTTPS, 443, ssf));

    final HttpParams params = client.getParams();
    HttpClientParams.setCookiePolicy(params, CookiePolicy.IGNORE_COOKIES);
    HttpProtocolParams.setContentCharset(params, UTF8);
    HttpConnectionParams.setConnectionTimeout(params, timeout);
    HttpConnectionParams.setSoTimeout(params, timeout);
    client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
    return new DecompressingHttpClient(client);
  }

  public static class NotModifiedException extends Exception {
    private static final long serialVersionUID = 1L;

    public NotModifiedException(String message) {
      super(message);
    }

  }

  private static class DefaultTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }
  }

  private static class DefaultHostnameVerifier implements X509HostnameVerifier {

    @Override
    public void verify(String string, SSLSocket ssls) throws IOException {
    }

    @Override
    public void verify(String string, X509Certificate xc) throws SSLException {
    }

    @Override
    public void verify(String string, String[] strings, String[] strings1) throws SSLException {
    }

    @Override
    public boolean verify(String string, SSLSession ssls) {
      return true;
    }
  }
}
