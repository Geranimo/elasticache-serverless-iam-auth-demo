package com.amazon.elasticache;

import java.time.Duration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import java.net.URI;
import java.time.Instant;

public class IAMAuthTokenRequest {
    private static final SdkHttpMethod REQUEST_METHOD = SdkHttpMethod.GET;
    private static final String REQUEST_PROTOCOL = "http://";
    private static final String PARAM_ACTION = "Action";
    private static final String PARAM_USER = "User";
    private static final String ACTION_NAME = "connect";
    private static final String SERVICE_NAME = "elasticache";
    private static final Duration TOKEN_EXPIRY_DURATION_SECONDS = Duration.ofSeconds(900);
    private static final String PARAM_RESOURCE_TYPE = "ResourceType";
    private static final String RESOURCE_TYPE_SERVERLESS_CACHE = "ServerlessCache";


    private final String userId;
    private final String cacheName;
    private final String region;

    public IAMAuthTokenRequest(String userId, String cacheName, String region) {
        this.userId = userId;
        this.cacheName = cacheName;
        this.region = region;
    }

    public String toSignedRequestUri(AwsCredentials credentials) {
        SdkHttpFullRequest request = getSignableRequest();

        // Sign the canonical request
        request = sign(request, credentials);

        // Return the signed URI
        return request.getUri().toString().replace(REQUEST_PROTOCOL, "");
    }

    private SdkHttpFullRequest getSignableRequest() {
        return SdkHttpFullRequest.builder()
            .method(REQUEST_METHOD)
            .uri(getRequestUri())
            .appendRawQueryParameter(PARAM_ACTION, ACTION_NAME)
            .appendRawQueryParameter(PARAM_USER, userId)
                .appendRawQueryParameter(PARAM_RESOURCE_TYPE, RESOURCE_TYPE_SERVERLESS_CACHE)
            .build();
    }

    private URI getRequestUri() {
        return URI.create(String.format("%s%s/", REQUEST_PROTOCOL, cacheName));
    }

    private SdkHttpFullRequest sign(SdkHttpFullRequest request, AwsCredentials credentials) {
        Instant expiryInstant = Instant.now().plus(TOKEN_EXPIRY_DURATION_SECONDS);
        Aws4Signer signer = Aws4Signer.create();
        Aws4PresignerParams signerParams = Aws4PresignerParams.builder()
            .signingRegion(Region.of(region))
            .awsCredentials(credentials)
            .signingName(SERVICE_NAME)
            .expirationTime(expiryInstant)
            .build();
        return signer.presign(request, signerParams);
    }
}