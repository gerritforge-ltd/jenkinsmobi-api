package mobi.jenkinsci.net;

import org.apache.http.client.HttpClient;

import java.net.MalformedURLException;
import java.net.URL;

public interface HttpClientFactory {
  HttpClient getHttpClient();

  HttpClient getBasicAuthHttpClient(URL url, String user, String password) throws MalformedURLException;
}
