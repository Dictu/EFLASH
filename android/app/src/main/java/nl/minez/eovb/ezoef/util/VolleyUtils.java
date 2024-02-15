/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.security.SecureVolleyRequestQueue;

public class VolleyUtils {

    public static class JsonBearerArrayRequest extends JsonRequest<JSONArray> {

        private final String bearer;

        public JsonBearerArrayRequest(int method, String url, String bearer, String requestBody, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
            super(method, url, requestBody, listener, errorListener);

            final RetryPolicy retryPolicy = new DefaultRetryPolicy(
                    SecureVolleyRequestQueue.DEFAULT_TIMEOUT_IN_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            );
            this.setRetryPolicy(retryPolicy);

            this.setShouldCache(false);

            this.bearer = bearer;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            final Map<String, String> headers = new HashMap<>(super.getHeaders());
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + bearer);

            if (Method.DELETE == this.getMethod()) {
                headers.put("Connection", "close"); // NOTE: Prevent 'Unexpected status line' errors
            }

            return headers;
        }

        @Override
        public Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
            if (response.statusCode == 204) {
                // Handle "No content"
                try {
                    return Response.success(new JSONArray("[]"), HttpHeaderParser.parseCacheHeaders(response));
                } catch (JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
            try {
                String jsonString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                return Response.success(new JSONArray(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }

    }

    public static class JsonBearerObjectRequest extends JsonRequest<JSONObject> {

        private final String bearer;

        public JsonBearerObjectRequest(int method, String url, String bearer, String requestBody, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(method, url, requestBody, listener, errorListener);

            final RetryPolicy retryPolicy = new DefaultRetryPolicy(
                    SecureVolleyRequestQueue.DEFAULT_TIMEOUT_IN_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            );
            this.setRetryPolicy(retryPolicy);

            this.setShouldCache(false);

            this.bearer = bearer;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            final Map<String, String> headers = new HashMap<>(super.getHeaders());
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + bearer);

            if (Method.DELETE == this.getMethod()) {
                headers.put("Connection", "close"); // NOTE: Prevent 'Unexpected status line' errors
            }

            return headers;
        }

        @Override
        public Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            if (response.statusCode == 204) {
                // Handle "No content"
                try {
                    return Response.success(new JSONObject("{}"), HttpHeaderParser.parseCacheHeaders(response));
                } catch (JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
            try {
                String jsonString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                return Response.success(new JSONObject(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }

    }

    public static VolleyError parseVolleyError(Context context, VolleyError volleyError) {
        final String errorMessage;
        if (volleyError instanceof TimeoutError) {
            errorMessage = context.getString(R.string.time_out_error);
        } else if (volleyError instanceof NoConnectionError) {
            errorMessage = context.getString(R.string.no_internet_connection);
        } else if (volleyError instanceof ServerError) {
            errorMessage = context.getString(R.string.server_error);
        } else if (volleyError instanceof NetworkError) {
            errorMessage = context.getString(R.string.network_error);
        } else if (volleyError instanceof AuthFailureError) {
            return volleyError;
        } else {
            errorMessage = context.getString(R.string.unknown_network_error);
        }

        return new VolleyError(errorMessage);
    }

}
