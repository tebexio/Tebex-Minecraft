package io.tebex.sdk;

import com.google.gson.*;
import com.intellectualsites.http.EntityMapper;
import com.intellectualsites.http.HttpClient;
import com.intellectualsites.http.external.GsonMapper;

public class HttpSdkBuilder {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final String apiUrl;
    private final EntityMapper entityMapper;

    public HttpSdkBuilder(String baseUrl) {
        this.apiUrl = baseUrl;
        this.entityMapper = EntityMapper.newInstance()
                .registerSerializer(JsonObject.class, GsonMapper.serializer(JsonObject.class, GSON))
                .registerDeserializer(JsonObject.class, GsonMapper.deserializer(JsonObject.class, GSON))
                .registerSerializer(JsonArray.class, GsonMapper.serializer(JsonArray.class, GSON))
                .registerDeserializer(JsonArray.class, GsonMapper.deserializer(JsonArray.class, GSON));
    }

    public EntityMapper getEntityMapper() {
        return entityMapper;
    }

    public HttpClient build() {
        return HttpClient.newBuilder()
                .withBaseURL(apiUrl)
                .withEntityMapper(mapper)
                .withDecorator(request -> request.withHeader("User-Agent", "Tebex-StoreSDK").withHeader("Content-Type", "application/json"))
                .build();
    }

    public Gson getGson() {
        return GSON;
    }
}
