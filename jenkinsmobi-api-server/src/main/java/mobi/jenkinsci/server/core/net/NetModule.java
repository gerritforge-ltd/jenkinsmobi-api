package mobi.jenkinsci.server.core.net;

import com.google.inject.AbstractModule;
import mobi.jenkinsci.net.HttpClientFactory;
import mobi.jenkinsci.plugin.URLDownloader;

public class NetModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(HttpClientFactory.class).to(PooledHttpClientFactory.class);
    bind(URLDownloader.class).to(HttpClientURLDownloader.class);

  }
}
