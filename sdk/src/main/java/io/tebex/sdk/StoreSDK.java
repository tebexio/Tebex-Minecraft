package io.tebex.sdk;

import com.google.gson.*;
import com.intellectualsites.http.EntityMapper;
import com.intellectualsites.http.HttpClient;
import com.intellectualsites.http.HttpResponse;
import com.intellectualsites.http.external.GsonMapper;
import io.tebex.sdk.exception.ServerNotFoundException;
import io.tebex.sdk.exception.ServerNotSetupException;
import io.tebex.sdk.obj.Package;
import io.tebex.sdk.obj.*;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.request.builder.CreateCouponRequest;
import io.tebex.sdk.request.exception.RateLimitException;
import io.tebex.sdk.request.response.DuePlayersResponse;
import io.tebex.sdk.request.response.OfflineCommandsResponse;
import io.tebex.sdk.request.response.PaginatedResponse;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.util.Pagination;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * The main StoreSDK class for interacting with the Tebex API.
 */
public class StoreSDK {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final HttpClient HTTP_CLIENT;
    private final HttpClient LEGACY_HTTP_CLIENT;
    private final String API_URL = "https://plugin.tebex.io";

    private final Platform platform;
    private String secretKey;

    /**
     * Constructs a new StoreSDK instance with the specified platform and secret key.
     *
     * @param platform  The platform on which the StoreSDK is running.
     * @param secretKey The secret key for authentication.
     */
    public StoreSDK(Platform platform, String secretKey) {
        this.platform = platform;
        this.secretKey = secretKey;

        final EntityMapper mapper = EntityMapper.newInstance()
                .registerSerializer(JsonObject.class, GsonMapper.serializer(JsonObject.class, GSON))
                .registerDeserializer(JsonObject.class, GsonMapper.deserializer(JsonObject.class, GSON))
                .registerSerializer(JsonArray.class, GsonMapper.serializer(JsonArray.class, GSON))
                .registerDeserializer(JsonArray.class, GsonMapper.deserializer(JsonArray.class, GSON))
                .registerDeserializer(CheckoutUrl.class, GsonMapper.deserializer(CheckoutUrl.class, GSON))
                ;

        this.HTTP_CLIENT = HttpClient.newBuilder()
                .withBaseURL(API_URL)
                .withEntityMapper(mapper)
                .withDecorator(request -> request.withHeader("User-Agent", "Tebex-StoreSDK").withHeader("Content-Type", "application/json"))
                .build();

        this.LEGACY_HTTP_CLIENT = HttpClient.newBuilder()
                .withBaseURL("https://plugin.buycraft.net")
                .withDecorator(request -> request.withHeader("User-Agent", "Tebex-StoreSDK").withHeader("Content-Type", "application/json"))
                .withEntityMapper(mapper)
                .build();
    }

    /**
     * Retrieves information about the server.
     *
     * @return A CompletableFuture that contains the ServerInformation object.
     */
    public CompletableFuture<ServerInformation> getServerInformation() {
        if (getSecretKey() == null) {
            CompletableFuture<ServerInformation> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/information")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if(req.getStatusCode() == 404 || req.getStatusCode() == 403) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if(req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else if(req.getStatusCode() != 200) {
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to retrieve server information"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);
            JsonObject account = jsonObject.getAsJsonObject("account");
            JsonObject server = jsonObject.getAsJsonObject("server");
            JsonObject currency = account.getAsJsonObject("currency");

            return new ServerInformation(
                    new ServerInformation.Store(
                            account.get("id").getAsInt(),
                            account.get("domain").getAsString(),
                            account.get("name").getAsString(),
                            new ServerInformation.Store.Currency(currency.get("iso_4217").getAsString(), currency.get("symbol").getAsString()),
                            account.get("online_mode").getAsBoolean(),
                            account.get("game_type").getAsString(), account.get("log_events").getAsBoolean()
                    ),
                    new ServerInformation.Server(server.get("id").getAsInt(), server.get("name").getAsString())
            );
        });
    }

    /**
     * Get the players who have commands due to be executed when they next login.
     *
     * @return A CompletableFuture that contains the DuePlayersResponse object.
     */
    public CompletableFuture<DuePlayersResponse> getDuePlayers() {
        if (getSecretKey() == null) {
            CompletableFuture<DuePlayersResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/queue")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if(req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if(req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else if(req.getStatusCode() != 200) {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to retrieve due players"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);
            JsonObject meta = jsonObject.get("meta").getAsJsonObject();
            JsonArray server = jsonObject.get("players").getAsJsonArray();

            List<QueuedPlayer> players = new ArrayList<>();
            for(JsonElement element : server) {
                JsonObject asJsonObject = element.getAsJsonObject();
                players.add(QueuedPlayer.fromJson(asJsonObject));
            }

            return new DuePlayersResponse(meta.get("execute_offline").getAsBoolean(), meta.get("next_check").getAsInt(), meta.get("more").getAsBoolean(), players);
        });
    }

    /**
     * Get the offline commands that are due to be executed.
     *
     * @return A CompletableFuture that contains the OfflineCommandsResponse object.
     */
    public CompletableFuture<OfflineCommandsResponse> getOfflineCommands() {
        if (getSecretKey() == null) {
            CompletableFuture<OfflineCommandsResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/queue/offline-commands")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404 || req.getStatusCode() == 403) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to retrieve offline commands"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);
            JsonObject meta = jsonObject.get("meta").getAsJsonObject();
            JsonArray commands = jsonObject.get("commands").getAsJsonArray();

            List<QueuedCommand> offlineCommands = new ArrayList<>();
            for (JsonElement element : commands) {
                JsonObject commandJson = element.getAsJsonObject();
                JsonObject conditions = commandJson.get("conditions").getAsJsonObject();

                QueuedPlayer queuedPlayer = QueuedPlayer.fromJson(commandJson.get("player").getAsJsonObject());
                int packageId = commandJson.get("package").isJsonNull() ? 0 : commandJson.get("package").getAsInt();
                int paymentId = commandJson.get("payment").isJsonNull() ? 0 : commandJson.get("payment").getAsInt();

                offlineCommands.add(new QueuedCommand(
                        commandJson.get("id").getAsInt(),
                        platform.getPlaceholderManager().handlePlaceholders(queuedPlayer, commandJson.get("command").getAsString()),
                        paymentId,
                        packageId,
                        conditions.get("delay").getAsInt(),
                        queuedPlayer
                ));
            }

            return new OfflineCommandsResponse(meta.get("limited").getAsBoolean(), offlineCommands);
        });
    }

    /**
     * Get the online commands that are due to be executed for a particular player.
     *
     * @param player The player for whom to retrieve commands.
     * @return A CompletableFuture that contains a list of QueuedCommand objects.
     */
    public CompletableFuture<List<QueuedCommand>> getOnlineCommands(QueuedPlayer player) {
        if (getSecretKey() == null) {
            CompletableFuture<List<QueuedCommand>> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/queue/online-commands/" + player.getId())
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404 || req.getStatusCode() == 403) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to retrieve online commands"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);
            JsonArray commands = jsonObject.getAsJsonArray("commands");

            List<QueuedCommand> queuedCommands = new ArrayList<>();
            for(JsonElement element : commands) {
                JsonObject commandJson = element.getAsJsonObject();
                JsonObject conditions = commandJson.getAsJsonObject("conditions");

                int packageId = commandJson.get("package").isJsonNull() ? 0 : commandJson.get("package").getAsInt();
                int paymentId = commandJson.get("payment").isJsonNull() ? 0 : commandJson.get("payment").getAsInt();
                queuedCommands.add(new QueuedCommand(
                        commandJson.get("id").getAsInt(),
                        platform.getPlaceholderManager().handlePlaceholders(player, commandJson.get("command").getAsString()),
                        paymentId,
                        packageId,
                        conditions.get("delay").getAsInt(),
                        conditions.get("slots").getAsInt()

                ));
            }

            return queuedCommands;
        });
    }

    /**
     * Delete one or more commands which have been executed on the game server.
     *
     * @param ids The IDs of the commands to delete.
     * @return A CompletableFuture that returns true if the commands were deleted successfully.
     */
    public CompletableFuture<Boolean> deleteCommands(List<Integer> ids) {
        if (getSecretKey() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        JsonArray idArray = new JsonArray();
        ids.forEach(idArray::add);

        JsonObject body = new JsonObject();
        body.add("ids", idArray);

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.delete("/queue")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .withInput(() -> GSON.toJson(body))
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404 || req.getStatusCode() == 403) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else if (req.getStatusCode() != 204) {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to delete commands"));
            }

            return true;
        });
    }

    /**
     * Get all community goals.
     *
     * @return A CompletableFuture that returns a list of CommunityGoal objects.
     */
    public CompletableFuture<List<CommunityGoal>> getCommunityGoals() {
        if (getSecretKey() == null) {
            CompletableFuture<List<CommunityGoal>> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/community_goals")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to get community goals"));
            }

            JsonArray jsonArray = response.getResponseEntity(JsonArray.class);

            return jsonArray.asList()
                    .stream()
                    .map(item -> CommunityGoal.fromJsonObject(item.getAsJsonObject()))
                    .collect(Collectors.toList());
        });
    }

    /**
     * Get a specific community goal.
     *
     * @param communityGoalId The ID of the community goal to retrieve.
     * @return A CompletableFuture that contains the CommunityGoal object.
     */
    public CompletableFuture<CommunityGoal> getCommunityGoal(int communityGoalId) {
        if (getSecretKey() == null) {
            CompletableFuture<CommunityGoal> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/community_goals/" + communityGoalId)
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to get community goal (ID: " + communityGoalId + ")"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);

            return CommunityGoal.fromJsonObject(jsonObject);
        });
    }

    /**
     * Create a checkout URL for a package.
     *
     * @param packageId The ID of the package for which to create a checkout URL.
     * @param username  The username of the user who will be checking out.
     * @return A CompletableFuture that contains the CheckoutUrl object.
     */
    public CompletableFuture<CheckoutUrl> createCheckoutUrl(int packageId, String username) {
        if (getSecretKey() == null) {
            CompletableFuture<CheckoutUrl> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("package_id", packageId);
        payload.addProperty("username", username);

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.post("/checkout")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .withInput(() -> payload)
                    .onStatus(201, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to create checkout url"));
            }

            return response.getResponseEntity(CheckoutUrl.class);
        });
    }

    /**
     * Get all coupons.
     *
     * @return A CompletableFuture that returns a PaginatedResponse of Coupon objects.
     */
    public CompletableFuture<PaginatedResponse<Coupon>> getCoupons() {
        if (getSecretKey() == null) {
            CompletableFuture<PaginatedResponse<Coupon>> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/coupons")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to delete commands"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);

            return new PaginatedResponse<>(
                    Pagination.fromJsonObject(jsonObject.getAsJsonObject("pagination")),
                    jsonObject.getAsJsonArray("data").asList()
                            .stream()
                            .map(item -> Coupon.fromJsonObject(item.getAsJsonObject()))
                            .collect(Collectors.toList())
            );
        });
    }

    /**
     * Get a specific coupon.
     *
     * @param id The ID of the coupon to retrieve.
     * @return A CompletableFuture that contains the Coupon object.
     */
    public CompletableFuture<Coupon> getCoupon(int id) {
        if (getSecretKey() == null) {
            CompletableFuture<Coupon> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/coupons/" + id)
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to delete commands"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);

            return Coupon.fromJsonObject(jsonObject.get("data").getAsJsonObject());
        });
    }

    /**
     * Create a coupon.
     *
     * @return A CompletableFuture that contains the new Coupon object.
     */
    public CompletableFuture<Coupon> createCoupon(CreateCouponRequest createCouponRequest) {
        if (getSecretKey() == null) {
            CompletableFuture<Coupon> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("code", createCouponRequest.getCode());
        payload.addProperty("effective_on", createCouponRequest.getEffectiveOn().name().toLowerCase());

        JsonArray idArray = new JsonArray();
        createCouponRequest.getEffectiveIds().forEach(idArray::add);

        if(createCouponRequest.getEffectiveOn() == CreateCouponRequest.EffectiveOn.PACKAGE) {
            payload.add("packages", idArray);
        } else if(createCouponRequest.getEffectiveOn() == CreateCouponRequest.EffectiveOn.CATEGORY) {
            payload.add("categories", idArray);
        } else {
            throw new RuntimeException("Invalid option selected");
        }

        payload.addProperty("discount_type", createCouponRequest.getDiscountType().name().toLowerCase());

        payload.addProperty("discount_percentage", createCouponRequest.getDiscountValue());
        payload.addProperty("discount_amount", createCouponRequest.getDiscountValue());

        payload.addProperty("redeem_unlimited", createCouponRequest.canRedeemUnlimited());
        payload.addProperty("expire_never", ! createCouponRequest.canExpire());

        if(! createCouponRequest.canRedeemUnlimited()) {
            payload.addProperty("expire_limit", createCouponRequest.getExpiryLimit());
        }

        if(createCouponRequest.canExpire()) {
            if(createCouponRequest.getExpiryDate() == null) {
                throw new RuntimeException("Coupon has expiry set to true, but no expiry date exists");
            }
            payload.addProperty("expire_date", createCouponRequest.getExpiryDate().toString());
        }

        payload.addProperty("start_date", createCouponRequest.getStartDate().toString());
        payload.addProperty("basket_type", createCouponRequest.getBasketType().name().toLowerCase());
        payload.addProperty("minimum", createCouponRequest.getMinimum());
        payload.addProperty("discount_application_method", createCouponRequest.getDiscountMethod().getValue());
        payload.addProperty("username", createCouponRequest.getUsername());
        payload.addProperty("note", createCouponRequest.getNote());

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.post("/coupons")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .withInput(() -> GSON.toJson(payload))
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404 || req.getStatusCode() == 403) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            JsonObject jsonObject = req.getResponseEntity(JsonObject.class);

                            if(jsonObject.has("error_message")) {
                                throw new CompletionException(new IOException(jsonObject.get("error_message").getAsString()));
                            }

                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to delete commands"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);

            return Coupon.fromJsonObject(jsonObject.getAsJsonObject("data"));
        });
    }

    /**
     * Get the store listing.
     *
     * @return A CompletableFuture that contains a List of Category objects.
     */
    public CompletableFuture<List<Category>> getListing() {
        if (getSecretKey() == null) {
            CompletableFuture<List<Category>> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/listing")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else {
                            JsonObject jsonObject = req.getResponseEntity(JsonObject.class);

                            if(jsonObject.has("error_message")) {
                                throw new CompletionException(new IOException(jsonObject.get("error_message").getAsString()));
                            }

                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to delete commands"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);

            return jsonObject.getAsJsonArray("categories")
                    .asList()
                    .stream()
                    .map(category -> Category.fromJsonObject(category.getAsJsonObject()))
                    .collect(Collectors.toList());
        });
    }

    /**
     * Delete a coupon.
     *
     * @param id The ID of the coupon to delete.
     * @return A CompletableFuture that returns true if the coupon was deleted successfully.
     */
    public CompletableFuture<Boolean> deleteCoupon(int id) {
        if (getSecretKey() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.delete("/coupons/" + id)
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else if (req.getStatusCode() != 204) {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to delete coupon"));
            }

            return true;
        });
    }

    public CompletableFuture<Boolean> sendEvents(List<ServerEvent> events) {
        if (getSecretKey() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.post("/events")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .withInput(() -> GSON.toJson(events))
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if (req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if (req.getStatusCode() == 429) {
                            throw new CompletionException(new RateLimitException("You are being rate limited."));
                        } else if (req.getStatusCode() != 204) {
                            platform.sendTriageEvent("Unexpected status code (" + req.getStatusCode() + ")");
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if (response == null) {
                throw new CompletionException(new IOException("Failed to send events"));
            }

            return true;
        });
    }

    /**
     * Get a specific package.
     *
     * @param id The ID of the package to retrieve.
     * @return A CompletableFuture that contains the Package object.
     */
    public CompletableFuture<Package> getPackage(int id) {
        if (getSecretKey() == null) {
            CompletableFuture<Package> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/package/" + id)
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if(req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if(req.getStatusCode() != 200) {
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to retrieve package"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);
            return Package.fromJsonObject(jsonObject);
        });
    }

    /**
     * Get all packages.
     *
     * @return A CompletableFuture that returns a List of Package objects.
     */
    public CompletableFuture<List<Package>> getPackages() {
        if (getSecretKey() == null) {
            CompletableFuture<List<Package>> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/packages")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if(req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if(req.getStatusCode() != 200) {
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to retrieve packages"));
            }

            JsonArray jsonArray = response.getResponseEntity(JsonArray.class);
            return jsonArray.asList().stream().map(item -> Package.fromJsonObject(item.getAsJsonObject())).collect(Collectors.toList());
        });
    }

    /**
     * Sends the current server telemetry to the Analyse API.
     *
     * @return A CompletableFuture that indicates whether the operation was successful.
     */
    public CompletableFuture<Boolean> sendTelemetry() {
        if (getSecretKey() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.LEGACY_HTTP_CLIENT.post("/analytics/startup")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if(req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if(req.getStatusCode() != 200) {
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to send telemetry"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);

            return jsonObject.get("success").getAsBoolean();
        });
    }

    /**
     * Bans a user from the webstore.
     *
     * @return A CompletableFuture that indicates whether the operation was successful.
     */
    public CompletableFuture<Boolean> createBan(String playerUUID, String ip, String reason) {
        if (getSecretKey() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("user", playerUUID);
        payload.addProperty("ip", ip);
        payload.addProperty("reason", reason);

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.post("/bans")
                    .withHeader("X-Tebex-Secret", secretKey)
                    .withInput(() -> GSON.toJson(payload))
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if(req.getStatusCode() == 404) {
                            throw new CompletionException(new ServerNotFoundException());
                        } else if(req.getStatusCode() != 422) {
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to create ban"));
            }

            return response.getStatusCode() == 200;
        });
    }

    /**
     * Looks up a player's information for this webstore.
     *
     * @param username The player's username or UUID
     * @return A CompletableFuture containing the relevant PlayerLookupInfo.
     */
    public CompletableFuture<PlayerLookupInfo> getPlayerLookupInfo(String username) {
        if (getSecretKey() == null) {
            CompletableFuture<PlayerLookupInfo> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/user/" + username)
                    .withHeader("X-Tebex-Secret", secretKey)
                    .onStatus(200, req -> {})
                    .onRemaining(req -> {
                        if(req.getStatusCode() != 404) {
                            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
                        }
                    })
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to retrieve player lookup info"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);
            return PlayerLookupInfo.fromJsonObject(jsonObject);
        });
    }

    /**
     * Get the secret key associated with this StoreSDK instance.
     *
     * @return The secret key as a String
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Set the secret key for this StoreSDK instance.
     *
     * @param secretKey The secret key as a String
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}