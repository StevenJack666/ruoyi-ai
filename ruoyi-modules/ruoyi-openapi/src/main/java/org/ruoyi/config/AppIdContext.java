package org.ruoyi.config;

/**
 * @author 30580113.zh
 */
public class AppIdContext {
    private static final ThreadLocal<String> appIdHolder = new ThreadLocal<>();

    public static void setAppId(String appId) {
        appIdHolder.set(appId);
    }

    public static String getAppId() {
        return appIdHolder.get();
    }

    public static void clear() {
        appIdHolder.remove();
    }
}
