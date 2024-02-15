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

import nl.minez.eovb.ezoef.model.Update;
import nl.minez.eovb.ezoef.util.MyTagHandler;
import nl.minez.eovb.ezoef.util.RelativeDateTimeUtils;
import nl.minez.eovb.ezoef.util.TypeFaceUtils;

public class UpdateViewModel extends BaseObservable {

    private Context context;
    private Update update;

    public UpdateViewModel(Context context, Update update) {
        this.context = context;
        this.update = update;
    }

    public String getTime() {
        return RelativeDateTimeUtils.relativeTimeTodayForDate(this.update.dateTime);
    }

    public Spanned getDescription() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(this.update.description, Html.FROM_HTML_MODE_LEGACY, null, new MyTagHandler());
        } else {
            return Html.fromHtml(this.update.description, null, new MyTagHandler());
        }
    }

    public Typeface getBoldTypeFace() {
        return TypeFaceUtils.themeBoldTypeFace(this.context);
    }

    public Typeface getRegularTypeFace() {
        return TypeFaceUtils.themeRegularTypeFace(this.context);
    }

}
