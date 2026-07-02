package org.ruoyi.common.core.utils.openapi;

import cn.hutool.core.date.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

    private static final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sfSimpleDate = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat sfSimpleTime = new SimpleDateFormat("HHmmss");
    private static final SimpleDateFormat sfSimpleDateHour = new SimpleDateFormat("yyyyMMdd_HH");
    private static final SimpleDateFormat sfUtcTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd");

    private static final Pattern patternIp = Pattern.compile("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}");

    private static final Pattern patternIpv6 = Pattern.compile("(^((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4}){1}|:))"
            + "|(([0-9A-Fa-f]{1,4}:){6}((:[0-9A-Fa-f]{1,4}){1}|"
            + "((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
            + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
            + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))|"
            + "(([0-9A-Fa-f]{1,4}:){5}((:[0-9A-Fa-f]{1,4}){1,2}|"
            + ":((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
            + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
            + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))|"
            + "(([0-9A-Fa-f]{1,4}:){4}((:[0-9A-Fa-f]{1,4}){1,3}"
            + "|:((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
            + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|[0-1][0-9][0-9]|"
            + "([0-9]){1,2})){3})|:))|(([0-9A-Fa-f]{1,4}:){3}((:[0-9A-Fa-f]{1,4}){1,4}|"
            + ":((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
            + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
            + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))|"
            + "(([0-9A-Fa-f]{1,4}:){2}((:[0-9A-Fa-f]{1,4}){1,5}|"
            + ":((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
            + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
            + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))"
            + "|(([0-9A-Fa-f]{1,4}:){1}((:[0-9A-Fa-f]{1,4}){1,6}"
            + "|:((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
            + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
            + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))|"
            + "(:((:[0-9A-Fa-f]{1,4}){1,7}|(:[fF]{4}){0,1}:((22[0-3]|2[0-1][0-9]|"
            + "[0-1][0-9][0-9]|([0-9]){1,2})"
            + "([.](25[0-5]|2[0-4][0-9]|[0-1][0-9][0-9]|([0-9]){1,2})){3})|:)))$)");


    static {
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        sfUtcTime.setTimeZone(tz);
        sf.setTimeZone(tz);
    }


    // 分隔字符串
    public static List<String> split(final String str, final String sep) {
        final List<String> res = new ArrayList<>(10);
        int pos, prev = 0;
        while ((pos = str.indexOf(sep, prev)) != -1) {
            res.add(str.substring(prev, pos));
            prev = pos + sep.length(); // start from next char after separator
        }
        res.add(str.substring(prev));

        return res;
    }

    public static String getNextDateString(String begDate) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sf1.parse(begDate));
        calendar.add(Calendar.DATE, 1);
        return sf1.format(calendar.getTime());
    }

    public static long getUnixtimeMilli(String time) {
        long unixtime = 0;
        try {
            if (time.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")) {
                unixtime = sf.parse(time).getTime();
            } else {
                unixtime = sf1.parse(time).getTime();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return unixtime;
    }

    public static long getStandardUTCUnixtimeMills(String time) {
        long unixtime = 0;
        try {
            if (time.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")) {
                unixtime = sf.parse(time).getTime();
            } else {
                unixtime = sf1.parse(time).getTime();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return unixtime - 8 * 3600 * 1000;
    }

    public static int getDayOfDate(String var) throws ParseException {
        Date date = sf1.parse(var);
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        return instance.get(Calendar.DAY_OF_YEAR);
    }

    public static String getStandUTC(String time) throws ParseException {
        DateTime dateTime;
        if (time.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")) {
            dateTime = new DateTime(sf.parse(time).getTime());
        } else {
            dateTime = new DateTime(sf1.parse(time).getTime());
        }

        return dateTime.toString("yyyy-MM-dd'T'HH:mm:ssZZ");
    }

    public static long ipCovertToLong(String ip) {
        long[] var = ipArr(ip);
        return (long) (var[0] << 24) +
                (long) (var[1] << 16) +
                (long) (var[2] << 8) +
                (long) var[3];
    }

    private static long[] ipArr(String ip) {
        String[] var = ip.split("\\.");
        long[] res = new long[4];
        for (int i = 0; i < var.length; ++i) {
            res[i] = Long.valueOf(var[i]);
        }
        return res;
    }
}
