/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.viewModel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spanned;
import android.view.View;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.model.Disruption;
import nl.minez.eovb.ezoef.util.FragmentUtils;
import nl.minez.eovb.ezoef.util.MyTagHandler;
import nl.minez.eovb.ezoef.util.RelativeDateTimeUtils;
import nl.minez.eovb.ezoef.util.TypeFaceUtils;
import nl.minez.eovb.ezoef.view.fragment.DisruptionDetailFragment;

public class DisruptionViewModel extends BaseObservable {

    private Context context;

    private Disruption disruption;

    public DisruptionViewModel(Context context, Disruption disruption) {
        this.context = context;
        this.disruption = disruption;
    }

    public Spanned getTitle() {
        return Html.fromHtml(this.disruption.title);
    }

    public Spanned getText() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(this.disruption.description, Html.FROM_HTML_MODE_LEGACY, null, new MyTagHandler());
        } else {
            return Html.fromHtml(this.disruption.description, null, new MyTagHandler());
        }
    }

    public String getUpdatedTime() {
        final DateTimeFormatter format = DateTimeFormat.forPattern("EE d MMM HH:mm").withLocale(new Locale("nl", "NL"));
        return this.context.getString(R.string.updated) + " " + format.print(this.disruption.getLastUpdateDateTime());
    }

    public String getCreatedTime() {
        final DateTimeFormatter format = DateTimeFormat.forPattern("EE d MMM HH:mm").withLocale(new Locale("nl", "NL"));
        return this.context.getString(R.string.created) + " " + format.print(this.disruption.dateTime);
    }

    public String getTime() {
        return RelativeDateTimeUtils.relativeTimeTodayForDate(this.disruption.dateTime);
    }

    public String getService() {
        return this.disruption.service.isEmpty() ? "-" : this.disruption.service;
    }

    public String getLocation() {
        return this.disruption.location.isEmpty() ? "-" : this.disruption.location;
    }

    public int getAdditionalInfoVisibility() {
        return this.disruption.updates.isEmpty() ? View.GONE : View.VISIBLE;
    }

    public Typeface getBoldTypeFace() {
        return TypeFaceUtils.themeBoldTypeFace(this.context);
    }

    public Typeface getRegularTypeFace() {
        return TypeFaceUtils.themeRegularTypeFace(this.context);
    }

    public View.OnClickListener onClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentUtils.addFragmentToBackStack(
                        context,
                        R.id.content_frame,
                        DisruptionDetailFragment.newInstance(disruption)
                );
            }
        };
    }
}
