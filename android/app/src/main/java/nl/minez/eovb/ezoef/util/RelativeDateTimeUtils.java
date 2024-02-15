/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.ocpsoft.prettytime.PrettyTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RelativeDateTimeUtils {

    public static String relativeTimeTodayForDate(Long dateTime) {
        final Locale locale = new Locale("nl");

        final Date now = new Date();
        final Date then = new Date(dateTime);

        if (Days.daysBetween(new DateTime(then), new DateTime(now)).getDays() >= 1) {
            return new SimpleDateFormat("E dd MMM", locale).format(then);
        }
        return new PrettyTime(locale).format(then);
    }

}
