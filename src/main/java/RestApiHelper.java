import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestApiHelper {

    private CloseableHttpClient client;
    private String token;

    public RestApiHelper() {
    }

    public RestApiHelper(String token) {
        this.token = token;
    }

    public String doPost(String url, Map<String, Object> params) throws IOException {
        HttpPost post = new HttpPost(url);

        if (token != null) {
            post.addHeader("Authentication", "Bearer " + token);
        }

        post.setEntity(new StringEntity(
                new Gson().toJson(params),
                ContentType.APPLICATION_JSON));
        return executeMethod(post);
    }

    public String doAuth(String url, Map<String, Object> params) throws IOException {
        return doPost(url, params);
    }

    public String doGet(String request) throws IOException {
        HttpGet get = new HttpGet(request);

        return executeMethod(get);
    }

    protected String executeMethod(final HttpUriRequest method) throws IOException {
        if (null == client) {
            RequestConfig config = RequestConfig.copy(RequestConfig.DEFAULT)
                    .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                    .build();
            client = HttpClients.custom()
                    .disableAuthCaching()
                    .setDefaultRequestConfig(config)
                    .build();
        }

        CloseableHttpResponse response = null;
        InputStream entity = null;
        try {
            method.setHeader("Accept-Charset", "UTF-8");
            method.setHeader("Accept", "application/json");
            response = client.execute(method);
            entity = response.getEntity().getContent();
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return IOUtils.toString(entity, "utf-8");
            } else {
                throw new IOException("HTTP request failed. Status: "
                        + response.getStatusLine().getReasonPhrase() + " Message:\n"
                        + IOUtils.toString(entity));
            }
        } finally {
            if (null != entity) {
                entity.close();
            }
            if (null != response) {
                response.close();
            }
        }
    }
}
