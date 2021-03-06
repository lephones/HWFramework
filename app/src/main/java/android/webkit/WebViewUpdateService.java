package android.webkit;

import android.os.RemoteException;

public final class WebViewUpdateService {
    private WebViewUpdateService() {
    }

    public static WebViewProviderInfo[] getAllWebViewPackages() {
        try {
            return getUpdateService().getAllWebViewPackages();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static WebViewProviderInfo[] getValidWebViewPackages() {
        try {
            return getUpdateService().getValidWebViewPackages();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCurrentWebViewPackageName() {
        try {
            return getUpdateService().getCurrentWebViewPackageName();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private static IWebViewUpdateService getUpdateService() {
        return WebViewFactory.getUpdateService();
    }
}
