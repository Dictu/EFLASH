/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import android.content.Context;
import android.graphics.Typeface;

public class TypeFaceUtils {

    private static final String RIJKSOVERHEID_SANS_TEXT_TT_BOLD_2_0_TTF = "fonts/RijksoverheidSansTextTT-Bold_2_0.ttf";
    private static final String RIJKSOVERHEID_SANS_TEXT_TT_REGULAR_2_0_TTF = "fonts/RijksoverheidSansTextTT-Regular_2_0.ttf";

    public static Typeface themeBoldTypeFace(Context context) {
        return Typeface.createFromAsset(context.getAssets(), RIJKSOVERHEID_SANS_TEXT_TT_BOLD_2_0_TTF);
    }

    public static Typeface themeRegularTypeFace(Context context) {
        return Typeface.createFromAsset(context.getAssets(), RIJKSOVERHEID_SANS_TEXT_TT_REGULAR_2_0_TTF);
    }

}
