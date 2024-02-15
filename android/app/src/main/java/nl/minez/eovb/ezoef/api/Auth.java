/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.api;

import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.security.PrivatePreferences;

public class Auth {

    public enum ErrorCode {
        UNKNOWN,
        FAILED,
        FORBIDDEN,
        BLOCKED,
        NOT_REGISTERED
    }

    public interface AuthHandler {
        void accessToken(String accessToken, ErrorCode errorCode, String errorMessage);
    }

    private static final String DEVICE_KEY = "DeviceKey";
    private static final String REFRESH_TOKEN_KEY = "RefreshTokenKey";

    private final Context context;
    private final PrivatePreferences privatePreferences;

    // Access tokens are not context dependent and should be shared through the whole app
    private static String accessToken = null;
    private static DateTime accessTokenExpiresAt = null;

    public Auth(Context context) {
        this.context = context;
        this.privatePreferences = new PrivatePreferences(context);
    }

    private String getDevice() {
        return this.privatePreferences.getString(DEVICE_KEY, null);
    }

    private String getRefreshToken() {
        return this.privatePreferences.getString(REFRESH_TOKEN_KEY, null);
    }

    public boolean isRegistered() {
        return this.getDevice() != null;
    }

    public void register(ApiConnector apiConnector, final AuthHandler authHandler) {
        this.invalidateAll();

        final String device;
        if (this.getDevice() != null) {
            device = this.getDevice();
        } else {
            device = UUID.randomUUID().toString();
        }

        // Register the device and email
        this.register(apiConnector, device, device + "@SSSSSSSSSSSSSSSS", authHandler);
    }

    public void register(ApiConnector apiConnector, final String email, final AuthHandler authHandler) {
        this.invalidateAll();

        final String device;
        if (this.getDevice() != null) {
            device = this.getDevice();
        } else {
            device = UUID.randomUUID().toString();
        }

        // Register the device and email
        this.register(apiConnector, device, email, authHandler);
    }

    private void register(ApiConnector apiConnector, final String device, String email, final AuthHandler authHandler) {
        apiConnector.register(device, email, new ApiConnector.Listener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    final ErrorCode errorCode = getAuthErrorCode(response);
                    if (errorCode != null) {
                        authHandler.accessToken(null, errorCode, getAuthErrorMessage(errorCode));
                    } else if (response.has("success") && response.getInt("success") != 0 && response.has("data")) {
                        final JSONObject data = response.getJSONObject("data");
                        if (data.has("refresh_token") && data.has("access_token") && data.has("expires_in")) {
                            privatePreferences.putString(DEVICE_KEY, device);
                            privatePreferences.putString(REFRESH_TOKEN_KEY, data.getString("refresh_token"));

                            final int expiresIn = data.getInt("expires_in");
                            accessTokenExpiresAt = new DateTime().plusSeconds(expiresIn);

                            accessToken = data.getString("access_token");
                            authHandler.accessToken(accessToken, null, null);
                        } else {
                            authHandler.accessToken(null, ErrorCode.FORBIDDEN, getAuthErrorMessage(ErrorCode.FORBIDDEN));
                        }
                    } else {
                        authHandler.accessToken(null, ErrorCode.UNKNOWN, getAuthErrorMessage(ErrorCode.UNKNOWN, response.getString("message")));
                    }
                } catch (JSONException e) {
                    Log.e(this.getClass().getSimpleName(), "Unable to parse JSON response: " + e);
                    authHandler.accessToken(null, ErrorCode.FAILED, getAuthErrorMessage(ErrorCode.FAILED));
                }
            }

            @Override
            public void onError(VolleyError volleyError) {
                final ErrorCode errorCode = getAuthErrorCode(volleyError.networkResponse);
                if (errorCode == null) {
                    authHandler.accessToken(null, ErrorCode.UNKNOWN, getAuthErrorMessage(ErrorCode.UNKNOWN, volleyError.getLocalizedMessage()));
                } else {
                    authHandler.accessToken(null, errorCode, getAuthErrorMessage(errorCode));
                }
            }
        });
    }

    public void accessToken(final ApiConnector apiConnector, final AuthHandler authHandler) {
        Log.i(this.getClass().getSimpleName(), "Expires at: " + (this.accessTokenExpiresAt == null ? 0 : this.accessTokenExpiresAt.getMillis() - new DateTime().getMillis()) / 1000);

        this.invalidateAccessTokenWhenExpired();

        if (this.accessToken != null) {
            // If access token was already set (and not expired), use this one
            authHandler.accessToken(this.accessToken, null, null);
            return;
        }

//        if (this.getRefreshToken() == null) {
//            //Deze check is niet nodig voor de publieke versie (eFlash)
//            if (!BuildConfig.TARGET_PUBLIC) {
//                authHandler.accessToken(null, ErrorCode.NOT_REGISTERED, getAuthErrorMessage(ErrorCode.NOT_REGISTERED));
//                return;
//            }
//        }

        final String oldRefreshToken = this.getRefreshToken();

        apiConnector.authRefreshToken(this.getRefreshToken(), new ApiConnector.Listener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.has("refresh_token")) {
                        // Update refresh token
                        privatePreferences.putString(REFRESH_TOKEN_KEY, response.getString("refresh_token"));
                    }

                    if (response.has("access_token") && response.has("expires_in")) {
                        final int expiresIn = response.getInt("expires_in");
                        accessTokenExpiresAt = new DateTime().plusSeconds(expiresIn);

                        accessToken = response.getString("access_token");
                        authHandler.accessToken(accessToken, null, null);
                    } else {
                        authHandler.accessToken(null, ErrorCode.UNKNOWN, getAuthErrorMessage(ErrorCode.UNKNOWN));
                    }
                } catch (JSONException e) {
                    Log.e(this.getClass().getSimpleName(), "Unable to parse JSON response: " + e);
                    authHandler.accessToken(null, ErrorCode.UNKNOWN, getAuthErrorMessage(ErrorCode.UNKNOWN));
                }
            }

            @Override
            public void onError(VolleyError volleyError) {
                final ErrorCode errorCode = getAuthErrorCode(volleyError.networkResponse);
                if (errorCode == null) {
                    authHandler.accessToken(null, ErrorCode.UNKNOWN, getAuthErrorMessage(ErrorCode.UNKNOWN, volleyError.getLocalizedMessage()));
                } else if (errorCode == Auth.ErrorCode.BLOCKED && getRefreshToken() != null && !getRefreshToken().equals(oldRefreshToken)) {
                    // Ignore this error when refresh tokens are renewed
                    accessToken(apiConnector, authHandler);
                } else {
                    authHandler.accessToken(null, errorCode, getAuthErrorMessage(errorCode));
                }
            }
        });
    }

    public String getAuthErrorMessage(ErrorCode errorCode) {
        return this.getAuthErrorMessage(errorCode, null);
    }

    public String getAuthErrorMessage(ErrorCode errorCode, String defaultErrorMessage) {
        final String errorMessage;
        if (errorCode == Auth.ErrorCode.FORBIDDEN) {
            errorMessage = context.getString(R.string.no_access_register_again);
        } else if (errorCode == Auth.ErrorCode.BLOCKED) {
            errorMessage = context.getString(R.string.not_active_activate_or_register);
        } else if (errorCode == Auth.ErrorCode.NOT_REGISTERED) {
            errorMessage = context.getString(R.string.not_registerd_use_business_email);
        } else if (errorCode == ErrorCode.UNKNOWN && defaultErrorMessage != null) {
            errorMessage = defaultErrorMessage;
        } else {
            errorMessage = this.context.getString(R.string.unknown_network_error);
        }

        return errorMessage;
    }

    public ErrorCode getAuthErrorCode(JSONObject response) {
        ErrorCode errorCode = null;
        try {
            if (response.has("success") && response.getInt("success") == 0) {
                errorCode = ErrorCode.FORBIDDEN;
            }
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to parse JSON response: " + e);
            errorCode = ErrorCode.UNKNOWN;
        }
        return errorCode;
    }

    public ErrorCode getAuthErrorCode(NetworkResponse response) {
        if (response != null) {
            if (response.statusCode == 400) {
                return ErrorCode.FORBIDDEN;
            } else if (response.statusCode == 403) {
                return ErrorCode.BLOCKED;
            }
        }
        return null;
    }

    private void invalidateAll() {
        this.accessToken = null;
        this.accessTokenExpiresAt = null;

        // Do not remove device (this is used as a user id in the backend and needs to be reused)
        this.privatePreferences.remove(REFRESH_TOKEN_KEY);
    }

    private void invalidateAccessTokenWhenExpired() {
        if (this.accessTokenExpiresAt == null || this.accessTokenExpiresAt.isBeforeNow()) {
            this.accessToken = null;
            this.accessTokenExpiresAt = null;
        }
    }

}