/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.minez.eovb.ezoef.api.ApiConnector;
import nl.minez.eovb.ezoef.api.Auth;
import nl.minez.eovb.ezoef.profile.Profile;
import nl.minez.eovb.ezoef.util.DialogUtils;
import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.util.LogUtil;

public class RegisterActivity extends BaseActivity {

    @BindView(R.id.emailEditText)
    EditText emailEditText;

    @BindView(R.id.registrationButton)
    Button registrationButton;

    @BindView(R.id.mainView)
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
        }

        if (!getResources().getBoolean(R.bool.is_tablet)) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_register);

        ButterKnife.bind(this);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary),
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark)});
        scrollView.setBackground(gd);

        this.registrationButton.setOnClickListener(new View.OnClickListener() {

            private boolean isValidEmail(CharSequence email) {
                return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
            }

            @Override
            public void onClick(View v) {
                // Invalidate user profile
                Profile.getInstance(RegisterActivity.this).invalidateAll();

                final Editable email = emailEditText.getText();
                if (isValidEmail(email)) {
                    final ProgressDialog progress = ProgressDialog.show(RegisterActivity.this, null, getString(R.string.registrating), true);

                    final ApiConnector apiConnector = ApiConnector.getInstance(RegisterActivity.this);
                    apiConnector.getAuth().register(apiConnector, email.toString(), new Auth.AuthHandler() {

                        @Override
                        public void accessToken(String accessToken, Auth.ErrorCode errorCode, String errorMessage) {
                            progress.hide();

                            if (errorCode == null) {
                                registrationSuccess();
                            } else {
                                cleanUp();

                                if (!DialogUtils.errorAlertDialogIsShowing(RegisterActivity.this)) {
                                    DialogUtils.createAuthErrorAlertDialog(RegisterActivity.this, errorCode, errorMessage, null).show();
                                }
                            }
                        }
                    });
                } else if (!DialogUtils.errorAlertDialogIsShowing(RegisterActivity.this)) {
                    DialogUtils.createErrorAlertDialog(RegisterActivity.this, getString(R.string.no_valid_email)).show();
                }
            }
        });
    }

    private void registrationSuccess() {
        this.hideKeyboard();

        new AlertDialog.Builder(this)
                .setTitle(R.string.registration_succesfull)
                .setMessage(R.string.activation_mail_sent)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void hideKeyboard() {
        final View view = this.getCurrentFocus();
        if (view != null) {
            final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Cancel all active requests from the api connector
        ApiConnector.getInstance(this).cancelAll();

        // Dismiss any open error dialog
        DialogUtils.dismissErrorAlertDialogIfShown(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.cleanUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtil.getInstance().logWithName(LogUtil.SCHERM, LogUtil.REGISTREER_SCHERM);
    }

    private void cleanUp() {
        this.emailEditText.setText(null);
        this.emailEditText.requestFocus();
    }

}