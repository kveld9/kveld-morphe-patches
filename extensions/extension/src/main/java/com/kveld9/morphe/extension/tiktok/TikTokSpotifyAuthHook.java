package com.kveld9.morphe.extension.tiktok;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.lang.reflect.Method;

/**
 * Routes the Spotify SDK single-sign-on handshake through Spotify's web OAuth flow.
 *
 * The bundled Spotify auth SDK only supports the Spotify app SSO flow
 * (com.spotify.sso.action.START_AUTH_FLOW). The Spotify app validates the caller's signing
 * certificate against the fingerprint registered for TikTok's client ID, so a re-signed APK
 * always gets an error back and "Add to Spotify" fails. Instead of starting the Spotify app,
 * the authorize page is loaded in a WebView hosted over the SDK LoginActivity and the
 * resulting code is handed back through the same onActivityResult contract the Spotify app uses.
 */
public final class TikTokSpotifyAuthHook {
    private static final String TAG = "MorpheTikTok";
    private static final String LOG_PREFIX = "[Fix Spotify Login] ";

    private static final String ACTION_START_AUTH_FLOW = "com.spotify.sso.action.START_AUTH_FLOW";
    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";

    private static final int RESULT_ERROR = -2;

    private TikTokSpotifyAuthHook() {}

    /**
     * Called at the start of TikTok's startActivityForResult(Activity) wrapper.
     * Returns true when the launch was consumed and must not reach the Spotify app.
     */
    public static boolean interceptStartActivityForResult(int requestCode, Activity activity, Intent intent) {
        if (activity == null || intent == null || !ACTION_START_AUTH_FLOW.equals(intent.getAction())) {
            return false;
        }
        String clientId = intent.getStringExtra("CLIENT_ID");
        String redirectUri = intent.getStringExtra("REDIRECT_URI");
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(redirectUri)) {
            Log.w(TAG, LOG_PREFIX + "Missing client id or redirect uri, falling back to Spotify app.");
            return false;
        }
        try {
            showAuthDialog(requestCode, activity, intent, clientId, redirectUri);
            Log.i(TAG, LOG_PREFIX + "Redirected Spotify SSO to web OAuth.");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, LOG_PREFIX + "Failed to open web OAuth: " + t.getMessage());
            return false;
        }
    }

    private static String buildAuthorizeUrl(Intent intent, String clientId, String redirectUri) {
        String responseType = intent.getStringExtra("RESPONSE_TYPE");
        Uri.Builder builder = Uri.parse(AUTHORIZE_URL).buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", TextUtils.isEmpty(responseType) ? "code" : responseType)
                .appendQueryParameter("redirect_uri", redirectUri);
        String[] scopes = intent.getStringArrayExtra("SCOPES");
        if (scopes != null && scopes.length > 0) {
            builder.appendQueryParameter("scope", TextUtils.join(" ", scopes));
        }
        String state = intent.getStringExtra("STATE");
        if (!TextUtils.isEmpty(state)) {
            builder.appendQueryParameter("state", state);
        }
        String campaign = intent.getStringExtra("UTM_CAMPAIGN");
        if (!TextUtils.isEmpty(campaign)) {
            builder.appendQueryParameter("utm_campaign", campaign);
        }
        builder.appendQueryParameter("utm_source", "spotify-sdk");
        builder.appendQueryParameter("utm_medium", "android-sdk");
        return builder.build().toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void showAuthDialog(final int requestCode, final Activity activity, Intent request,
                                       String clientId, final String redirectUri) {
        final boolean[] delivered = new boolean[1];
        final WebView webView = new WebView(activity);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar) {
            @Override
            public void onBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    super.onBackPressed();
                }
            }
        };

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return handleUrl(req.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (handleUrl(Uri.parse(url))) {
                    view.stopLoading();
                    return;
                }
                super.onPageStarted(view, url, favicon);
            }

            private boolean handleUrl(Uri uri) {
                if (uri == null || !isRedirect(uri, redirectUri)) {
                    return false;
                }
                if (!delivered[0]) {
                    delivered[0] = true;
                    deliverRedirect(requestCode, activity, uri);
                    dismissQuietly(dialog);
                }
                return true;
            }
        });

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setOnCancelListener(d -> {
            if (!delivered[0]) {
                delivered[0] = true;
                Log.i(TAG, LOG_PREFIX + "Web OAuth cancelled by user.");
                deliverResult(requestCode, activity, Activity.RESULT_CANCELED, null);
            }
        });
        dialog.setOnDismissListener(d -> webView.destroy());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        webView.loadUrl(buildAuthorizeUrl(request, clientId, redirectUri));
        dialog.show();
    }

    private static boolean isRedirect(Uri uri, String redirectUri) {
        Uri expected = Uri.parse(redirectUri);
        return TextUtils.equals(uri.getScheme(), expected.getScheme())
                && TextUtils.equals(uri.getHost(), expected.getHost())
                && TextUtils.equals(uri.getPath(), expected.getPath());
    }

    private static void deliverRedirect(int requestCode, Activity activity, Uri uri) {
        String code = uri.getQueryParameter("code");
        String error = uri.getQueryParameter("error");
        Intent data = new Intent();
        if (!TextUtils.isEmpty(code)) {
            Bundle reply = new Bundle();
            reply.putString("RESPONSE_TYPE", "code");
            reply.putString("AUTHORIZATION_CODE", code);
            reply.putString("STATE", uri.getQueryParameter("state"));
            data.putExtra("REPLY", reply);
            Log.i(TAG, LOG_PREFIX + "Web OAuth returned authorization code.");
            deliverResult(requestCode, activity, Activity.RESULT_OK, data);
        } else {
            data.putExtra("ERROR", TextUtils.isEmpty(error) ? "Missing authorization code" : error);
            Log.w(TAG, LOG_PREFIX + "Web OAuth returned error: " + error);
            deliverResult(requestCode, activity, RESULT_ERROR, data);
        }
    }

    private static void deliverResult(int requestCode, Activity activity, int resultCode, Intent data) {
        try {
            Method method;
            try {
                method = activity.getClass().getMethod("onActivityResult", int.class, int.class, Intent.class);
            } catch (NoSuchMethodException e) {
                method = Activity.class.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
            }
            method.setAccessible(true);
            method.invoke(activity, requestCode, resultCode, data);
        } catch (Throwable t) {
            Log.w(TAG, LOG_PREFIX + "Failed to deliver auth result: " + t.getMessage());
            activity.finish();
        }
    }

    private static void dismissQuietly(Dialog dialog) {
        try {
            dialog.dismiss();
        } catch (Throwable ignored) {
        }
    }
}
