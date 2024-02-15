/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.minez.eovb.BuildConfig;
import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.receiver.NetworkChangeReceiver;
import nl.minez.eovb.ezoef.security.SecureVolleyRequestQueue;
import nl.minez.eovb.ezoef.util.DataUtils;
import nl.minez.eovb.ezoef.util.DialogUtils;
import nl.minez.eovb.ezoef.util.JsonUtils;
import nl.minez.eovb.ezoef.util.VolleyUtils;
import nl.minez.eovb.ezoef.view.activity.RegisterActivity;

public class ApiConnector {

    public interface Listener<T> {
        void onSuccess(T response);

        void onError(VolleyError error);
    }

    private static final String BASE_URL = BuildConfig.SERVER_URL;
    private static final String V1 = BASE_URL + "api/v1/";

    private static final HashMap<Context, ApiConnector> instances = new HashMap<>();

    private final Context context;
    private final RequestQueue requestQueue;
    private final Auth auth;

    private BroadcastReceiver reachabilityChangedFilterReceiver;
    private ArrayList<String> refreshingAccessTokenEndPoints = new ArrayList<>();

    private ApiConnector(final Context context) {
        VolleyLog.DEBUG = BuildConfig.DEBUG;

        this.context = context;
//        this.requestQueue = SecureVolleyRequestQueue.getInstance(context).getSecureRequestQueue();
        this.requestQueue = Volley.newRequestQueue(context);
        this.auth = new Auth(context);
    }

    public static synchronized ApiConnector getInstance(final Context context) {
        ApiConnector instance = instances.get(context);
        if (instance == null) {
            instance = new ApiConnector(context);
            instances.put(context, instance);
        }
        return instance;
    }

    private void checkForReachability(Context context) {
        if (!DataUtils.isNetworkAvailable(context)) {
            if (!DialogUtils.errorAlertDialogIsShowing(this.context)) {
                DialogUtils.createErrorAlertDialog(this.context, R.string.no_internet_connection).show();
            }
        }
    }

    private void handleAuthError(Auth.ErrorCode errorCode, String errorMessage, Listener errorListener) {
        if (!DialogUtils.errorAlertDialogIsShowing(this.context)) {
            DialogUtils.createAuthErrorAlertDialog(this.context, errorCode, errorMessage, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!(context instanceof RegisterActivity)) {
                        // Show registration screen after pressing "Register"
                        final Intent intent = new Intent(context, RegisterActivity.class);
                        context.startActivity(intent);
                    }
                }
            }).show();
        }

        if (errorListener != null) {
            errorListener.onError(new VolleyError(errorMessage));
        }
    }

    public Auth getAuth() {
        return this.auth;
    }

    public void listenForReachability() {
        this.checkForReachability(context);

        if (this.reachabilityChangedFilterReceiver == null) {
            this.reachabilityChangedFilterReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    checkForReachability(context);
                }
            };
            this.context.registerReceiver(this.reachabilityChangedFilterReceiver, new IntentFilter(NetworkChangeReceiver.REACHABILITY_CHANGED_FILTER_BROADCAST));
        }
    }

    public void unlistenForReachability() {
        if (this.reachabilityChangedFilterReceiver != null) {
            this.context.unregisterReceiver(this.reachabilityChangedFilterReceiver);
            this.reachabilityChangedFilterReceiver = null;
        }
    }

    public void register(final String device, final String email, final Listener<JSONObject> listener) {
        try {
            final JSONObject jsonRequest = new JSONObject("{" +
                    "\"name\":\"" + device + "\"," +
                    "\"mail\":\"" + email + "\"" +
                    "}"
            );

            final JsonObjectRequest request = new JsonObjectRequest(V1 + "register?_format=json", jsonRequest,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            listener.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            listener.onError(VolleyUtils.parseVolleyError(ApiConnector.this.context, error));
                        }
                    }) {
            };

            final RetryPolicy retryPolicy = new DefaultRetryPolicy(
                    SecureVolleyRequestQueue.DEFAULT_TIMEOUT_IN_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            );
            request.setRetryPolicy(retryPolicy);

            request.setShouldCache(false);

            this.requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to create JSON request: " + e);
        }
    }

    public void authRefreshToken(final String refreshToken, final Listener<JSONObject> listener) {
        try {
            final JSONObject jsonRequest = new JSONObject("{}");

            final JsonRequest<JSONObject> request = new VolleyUtils.JsonBearerObjectRequest(Request.Method.GET, BASE_URL + "simple-oauth/refresh", refreshToken, jsonRequest.toString(),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            listener.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            listener.onError(VolleyUtils.parseVolleyError(ApiConnector.this.context, error));
                        }
                    }) {
            };

            this.requestQueue.add(request);

        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to create JSON request: " + e);
        }
    }

    public void registerForPushNotifications(final String pushToken, final boolean updatesOn, final List<Long> locations, final Listener<JSONObject> listener) {
        try {
            final JSONObject jsonRequest = new JSONObject("{" +
                    "\"token\":\"" + pushToken + "\"," +
                    "\"type\":\"android\"," +
                    "\"messagetypes\":[" + JsonUtils.combineWithSeparator(",", locations.toArray()) + "]," +
                    "\"notifications\":{updates:" + (updatesOn ? "1" : "0") + "}" +
                    "}"
            );

            this.doObjectRequest(Request.Method.POST, "push_notifications?_format=json", jsonRequest, listener);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to create JSON request: " + e);
        }
    }

    public void unregisterForPushNotifications(final String pushToken, final Listener<JSONObject> listener) {
        try {
            final JSONObject jsonRequest = new JSONObject("{" +
                    "\"_format\":\"hal_json\"" +
                    "}"
            );

            this.doObjectRequest(Request.Method.DELETE, "push_notifications/" + pushToken, jsonRequest, listener);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to create JSON request: " + e);
        }
    }

    public void openDisruptions(final List<Long> services, final List<Long> locations, final Listener<JSONArray> listener) {
        try {
            final ArrayList<Long> combinedList = new ArrayList<>(services);
            combinedList.addAll(locations);

            final JSONObject jsonRequest = new JSONObject("{" +
                    "\"type\":[{\"target_id\":\"incident\"}]," +
                    "\"state\":[\"open\"]," +
                    "\"messagetypes\":[" + JsonUtils.combineWithSeparator(",", combinedList.toArray()) + "]" +
                    "}"
            );
            this.doArrayRequest(Request.Method.POST, "incidents?_format=json", jsonRequest, listener);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to create JSON request: " + e);
        }
    }

    public void closedDisruptions(final List<Long> services, final List<Long> locations, final Listener<JSONArray> listener) {
        try {
            final ArrayList<Long> combinedList = new ArrayList<>(services);
            combinedList.addAll(locations);

            final JSONObject jsonRequest = new JSONObject("{" +
                    "\"type\":[{\"target_id\":\"incident\"}]," +
                    "\"state\":[\"closed\"]," +
                    "\"messagetypes\":[" + JsonUtils.combineWithSeparator(",", combinedList.toArray()) + "]" +
                    "}"
            );
            this.doArrayRequest(Request.Method.POST, "incidents?_format=json", jsonRequest, listener);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to create JSON request: " + e);
        }
    }

    public void announcements(final List<Long> services, final List<Long> locations, final Listener<JSONArray> listener) {

        try {
            final ArrayList<Long> combinedList = new ArrayList<>(services);
            combinedList.addAll(locations);

            final JSONObject jsonRequest = new JSONObject("{" +
                    "\"type\":[{\"target_id\":\"announcement\"}]," +
                    "\"state\":[\"open\"]," +
                    "\"messagetypes\":[" + JsonUtils.combineWithSeparator(",", combinedList.toArray()) + "]" +
                    "}"
            );
            this.doArrayRequest(Request.Method.POST, "announcements?_format=json", jsonRequest, listener);

        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to create JSON request: " + e);
        }
    }


    public void locations(final Listener<JSONObject> listener) {
        try {
            this.doObjectRequest(Request.Method.GET, "messagetypes?_format=json", new JSONObject("{}"), listener);

        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Unable to create JSON request: " + e);
        }
    }

    private String getEndpointDescription(String endpoint, JSONObject jsonRequest) {
        return endpoint + ":" + jsonRequest.toString();
    }

    public void doArrayRequest(final int method, final String endpoint, final JSONObject jsonRequest, final Listener<JSONArray> listener) {
        // Try to get an access token
        this.auth.accessToken(this, new Auth.AuthHandler() {
            @Override
            public void accessToken(String accessToken, Auth.ErrorCode errorCode, String errorMessage) {
                final String endpointDescription = getEndpointDescription(endpoint, jsonRequest);

                if (errorCode != null) {
                    refreshingAccessTokenEndPoints.remove(endpointDescription);
                    handleAuthError(errorCode, errorMessage, listener);
                    return;
                }

                final JsonRequest<JSONArray> request = new VolleyUtils.JsonBearerArrayRequest(method, V1 + endpoint, accessToken, jsonRequest.toString(),
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                refreshingAccessTokenEndPoints.remove(endpointDescription);
                                listener.onSuccess(response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                final VolleyError volleyError = VolleyUtils.parseVolleyError(ApiConnector.this.context, error);
                                final Auth.ErrorCode errorCode = auth.getAuthErrorCode(error.networkResponse);

                                if (errorCode == null) {
                                    refreshingAccessTokenEndPoints.remove(endpointDescription);
                                    ((Listener) listener).onError(volleyError);
                                } else if (errorCode == Auth.ErrorCode.BLOCKED && !refreshingAccessTokenEndPoints.contains(endpointDescription)) {
                                    refreshingAccessTokenEndPoints.add(endpointDescription);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            doArrayRequest(method, endpoint, jsonRequest, listener);
                                        }
                                    }, 1000);
                                } else {
                                    refreshingAccessTokenEndPoints.remove(endpointDescription);
                                    // The last attempt to refresh the token using an endpoint refresh failed
                                    handleAuthError(errorCode, auth.getAuthErrorMessage(errorCode, volleyError.getLocalizedMessage()), listener);
                                }
                            }
                        }) {
                };

                requestQueue.add(request);
            }
        });
    }

    public void doObjectRequest(final int method, final String endpoint, final JSONObject jsonRequest, final Listener<JSONObject> listener) {
        // Try to get an access token
        this.auth.accessToken(this, new Auth.AuthHandler() {
            @Override
            public void accessToken(String accessToken, Auth.ErrorCode errorCode, String errorMessage) {
                final String endpointDescription = getEndpointDescription(endpoint, jsonRequest);

                if (errorCode != null) {
                    refreshingAccessTokenEndPoints.remove(endpointDescription);
                    handleAuthError(errorCode, errorMessage, listener);
                    return;
                }

                final JsonRequest<JSONObject> request = new VolleyUtils.JsonBearerObjectRequest(method, V1 + endpoint, accessToken, jsonRequest.toString(),
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                refreshingAccessTokenEndPoints.remove(endpointDescription);

                                final Auth.ErrorCode authErrorCode = auth.getAuthErrorCode(response);
                                if (authErrorCode == null) {
                                    listener.onSuccess(response);
                                } else {
                                    handleAuthError(authErrorCode, auth.getAuthErrorMessage(authErrorCode), listener);
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                final VolleyError volleyError = VolleyUtils.parseVolleyError(ApiConnector.this.context, error);
                                final Auth.ErrorCode errorCode = auth.getAuthErrorCode(error.networkResponse);
                                if (errorCode == null) {
                                    refreshingAccessTokenEndPoints.remove(endpointDescription);
                                    ((Listener) listener).onError(volleyError);
                                } else if (errorCode == Auth.ErrorCode.BLOCKED && !refreshingAccessTokenEndPoints.contains(endpointDescription)) {
                                    refreshingAccessTokenEndPoints.add(endpointDescription);
                                    auth.accessToken(ApiConnector.this, new Auth.AuthHandler() {
                                        @Override
                                        public void accessToken(String accessToken, Auth.ErrorCode errorCode, String errorMessage) {
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    doObjectRequest(method, endpoint, jsonRequest, listener);
                                                }
                                            }, 1000);
                                        }
                                    });
                                } else {
                                    refreshingAccessTokenEndPoints.remove(endpointDescription);
                                    // The last attempt to refresh the token using an endpoint refresh failed
                                    handleAuthError(errorCode, auth.getAuthErrorMessage(errorCode, volleyError.getLocalizedMessage()), listener);
                                }
                            }
                        }) {
                };

                requestQueue.add(request);
            }
        });
    }

    public void cancelAll() {
        this.requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

}