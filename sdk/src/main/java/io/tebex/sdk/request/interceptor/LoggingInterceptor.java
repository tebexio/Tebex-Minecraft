package io.tebex.sdk.request.interceptor;

import io.tebex.sdk.Tebex;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LoggingInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        if (Tebex.get().getPlatformConfig().isVerbose()) {
            RequestBody requestBody = request.body();
            String requestBodyString = null;

            if (requestBody != null) {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    requestBodyString = buffer.readString(contentType.charset());
                }
            }

            Tebex.get().debug(String.format(" -> %1$s %2$s | %3$s",
                    request.method(), request.url(),
                    requestBodyString != null ? requestBodyString : "No body"));
        }

        return chain.proceed(request);
    }
}
