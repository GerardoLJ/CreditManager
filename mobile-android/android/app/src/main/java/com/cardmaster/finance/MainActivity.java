package com.cardmaster.finance;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import com.getcapacitor.BridgeActivity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends BridgeActivity {

    public class AndroidStorageBridge {
        private Context context;

        public AndroidStorageBridge(Context ctx) {
            this.context = ctx;
        }

        @JavascriptInterface
        public String saveDbFile(String base64Data, String filename) {
            try {
                String cleanBase64 = base64Data != null ? base64Data.replaceFirst("^data:.*?;base64,", "").trim() : "";
                byte[] bytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                File file = new File(context.getExternalFilesDir(null), filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
                return file.getAbsolutePath();
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }

        @JavascriptInterface
        public String loadDbFile(String filename) {
            try {
                File file = new File(context.getExternalFilesDir(null), filename);
                if (!file.exists()) return "";
                FileInputStream fis = new FileInputStream(file);
                byte[] bytes = new byte[(int) file.length()];
                fis.read(bytes);
                fis.close();
                return Base64.encodeToString(bytes, Base64.NO_WRAP);
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String getDbFilePath(String filename) {
            File file = new File(context.getExternalFilesDir(null), filename);
            return file.exists() ? file.getAbsolutePath() : new File(context.getExternalFilesDir(null), filename).getAbsolutePath();
        }

        @JavascriptInterface
        public boolean hasDbFile(String filename) {
            File file = new File(context.getExternalFilesDir(null), filename);
            return file.exists() && file.length() > 0;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getWindow() != null) {
            getWindow().setStatusBarColor(Color.parseColor("#0b1329"));
            getWindow().setNavigationBarColor(Color.parseColor("#0b1329"));
        }

        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().addJavascriptInterface(new AndroidStorageBridge(this), "AndroidNativeStorage");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().addJavascriptInterface(new AndroidStorageBridge(this), "AndroidNativeStorage");
        }
    }
}
