package com.tinify;

import com.google.gson.Gson;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Client {
    private OkHttpClient client;
    private String credentials;
    private String userAgent;

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    public static final String API_ENDPOINT = "https://api.tinify.com";
    public static final String USER_AGENT = "Tinify/"
            + Client.class.getPackage().getImplementationVersion()
            + " Java/" + System.getProperty("java.version")
            + " (" + System.getProperty("java.vendor")
            + ", " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + ")";

    public enum Method {
        POST,
        GET
    }

    public Client(final String key, final String appIdentifier) {
        client = new OkHttpClient()
        	.newBuilder()
        	.readTimeout(0, TimeUnit.SECONDS)
        	.writeTimeout(0, TimeUnit.SECONDS)
        	.connectTimeout(0, TimeUnit.SECONDS)
        	.sslSocketFactory(SSLContext.getSocketFactory())
        	.build();
//        client.setSslSocketFactory(SSLContext.getSocketFactory());
//        client.setConnectTimeout(0, TimeUnit.SECONDS);
//        client.setReadTimeout(0, TimeUnit.SECONDS);
//        client.setWriteTimeout(0, TimeUnit.SECONDS);

        credentials = Credentials.basic("api", key);
        if (appIdentifier == null) {
            userAgent = USER_AGENT;
        } else {
            userAgent = USER_AGENT + " " + appIdentifier;
        }
    }

    public final Response request(final Method method, final String endpoint) throws Exception {
        /* OkHttp does not support null request bodies if the method is POST. */
        if (method.equals(Method.POST)) {
            return request(method, endpoint, RequestBody.create(null, new byte[] {}));
        } else {
            return request(method, endpoint, (RequestBody) null);
        }
    }

    public final Response request(final Method method, final String endpoint, final Options options) throws Exception {
        if (method.equals(Method.GET)) {
            return request(method, endpoint, options.isEmpty() ? null : RequestBody.create(JSON, options.toJson()));
        } else {
            return request(method, endpoint, RequestBody.create(JSON, options.toJson()));
        }
    }

    public final Response request(final Method method, final String endpoint, final byte[] body) throws Exception {
        return request(method, endpoint, RequestBody.create(null, body));
    }

    private Response request(final Method method, final String endpoint, final RequestBody body) throws Exception {
        HttpUrl url;
        if (endpoint.startsWith("https")) {
            url = HttpUrl.parse(endpoint);
        } else {
            url = HttpUrl.parse(API_ENDPOINT + endpoint);
        }

        Request request = new Request.Builder()
                .header("Authorization", credentials)
                .header("User-Agent", userAgent)
                .url(url)
                .method(method.toString(), body)
                .build();

        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (java.lang.Exception e) {
            throw new ConnectionException("Error while connecting: " + e.getMessage(), e);
        }

        String compressionCount = response.header("Compression-Count");
        if (compressionCount != null && !compressionCount.isEmpty()) {
            Tinify.setCompressionCount(Integer.valueOf(compressionCount));
        }

        if (response.isSuccessful()) {
            return response;
        } else {
            Exception.Data data;
            Gson gson = new Gson();
             try {
                 data = gson.fromJson(response.body().charStream(), Exception.Data.class);
            } catch (com.google.gson.JsonParseException e) {
                 data = new Exception.Data();
                 data.setMessage("Error while parsing response: " + e.getMessage());
                 data.setError("ParseError");
            }
            throw Exception.create(
                    data.getMessage(),
                    data.getError(),
                    response.code());
        }
    }
}
