package com.cardmaster.finance;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import androidx.documentfile.provider.DocumentFile;
import com.getcapacitor.BridgeActivity;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends BridgeActivity {

    private static final int REQUEST_CODE_PICK_FOLDER = 9901;
    private static final int REQUEST_CODE_PICK_FILE = 9902;
    private static final int REQUEST_CODE_SAVE_FILE_AS = 9903;

    private byte[] pendingSaveBytes = null;

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public class AndroidStorageBridge {
        private Context context;

        public AndroidStorageBridge(Context ctx) {
            this.context = ctx;
        }

        @JavascriptInterface
        public void pickFolder() {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                              | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                MainActivity.this.startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
            } catch (Exception e) {
                final String errJson = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                runOnUiThread(() -> {
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        getBridge().getWebView().evaluateJavascript("if (window.onAndroidFolderPicked) window.onAndroidFolderPicked(" + errJson + ");", null);
                    }
                });
            }
        }

        @JavascriptInterface
        public void pickFile() {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                MainActivity.this.startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
            } catch (Exception e) {
                final String errJson = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                runOnUiThread(() -> {
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        getBridge().getWebView().evaluateJavascript("if (window.onAndroidFilePicked) window.onAndroidFilePicked(" + errJson + ");", null);
                    }
                });
            }
        }

        @JavascriptInterface
        public boolean isStorageConfigured() {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            String uri = prefs.getString("selected_tree_uri", null);
            boolean defaultChosen = prefs.getBoolean("default_storage_chosen", false);
            return (uri != null && !uri.isEmpty()) || defaultChosen;
        }

        @JavascriptInterface
        public void setDefaultStorageChosen(boolean chosen) {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("default_storage_chosen", chosen).apply();
        }

        @JavascriptInterface
        public String getStorageInfo() {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            String uri = prefs.getString("selected_tree_uri", "");
            String name = prefs.getString("selected_folder_name", "");
            boolean defaultChosen = prefs.getBoolean("default_storage_chosen", false);
            boolean configured = isStorageConfigured();
            boolean hasDb = hasDbFile("tarjetas.db");
            long sizeBytes = getDbFileSize("tarjetas.db");

            String folderDisplay = name;
            if (folderDisplay.isEmpty()) {
                folderDisplay = "Descargas";
            }
            String display = !name.isEmpty() ? name : "Descargas (Almacenamiento del Teléfono)";
            if (hasDb && !configured) {
                configured = true;
            }
            return "{\"configured\":" + configured + ",\"folderName\":\"" + escapeJson(folderDisplay) + "\",\"pathDisplay\":\"" + escapeJson(display) + "\",\"hasDb\":" + hasDb + ",\"sizeBytes\":" + sizeBytes + "}";
        }

        @JavascriptInterface
        public void resetStorageLocation() {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            prefs.edit().remove("selected_tree_uri").remove("selected_folder_name").remove("default_storage_chosen").apply();
        }

        @JavascriptInterface
        public void saveFileAs(String base64Data, String filename) {
            try {
                String cleanBase64 = base64Data != null ? base64Data.replaceFirst("^data:.*?;base64,", "").trim() : "";
                pendingSaveBytes = Base64.decode(cleanBase64, Base64.DEFAULT);

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/x-sqlite3");
                intent.putExtra(Intent.EXTRA_TITLE, filename != null && !filename.isEmpty() ? filename : "tarjetas.db");
                MainActivity.this.startActivityForResult(intent, REQUEST_CODE_SAVE_FILE_AS);
            } catch (Exception e) {
                final String errJson = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                runOnUiThread(() -> {
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        getBridge().getWebView().evaluateJavascript("if (window.onAndroidFileSaved) window.onAndroidFileSaved(" + errJson + ");", null);
                    }
                });
            }
        }

        @JavascriptInterface
        public String saveDbFile(String base64Data, String filename) {
            try {
                String cleanBase64 = base64Data != null ? base64Data.replaceFirst("^data:.*?;base64,", "").trim() : "";
                byte[] bytes = Base64.decode(cleanBase64, Base64.DEFAULT);

                SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
                String treeUriStr = prefs.getString("selected_tree_uri", null);

                String savedPath = "";

                // 1. Guardar en la carpeta física seleccionada por el usuario vía SAF
                if (treeUriStr != null && !treeUriStr.isEmpty()) {
                    try {
                        Uri treeUri = Uri.parse(treeUriStr);
                        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
                        if (pickedDir != null && pickedDir.exists() && pickedDir.canWrite()) {
                            DocumentFile dbFile = pickedDir.findFile(filename);
                            if (dbFile == null) {
                                dbFile = pickedDir.createFile("application/x-sqlite3", filename);
                            }
                            if (dbFile != null) {
                                OutputStream os = null;
                                try {
                                    os = context.getContentResolver().openOutputStream(dbFile.getUri(), "wt");
                                } catch (Exception eWt) {
                                    os = context.getContentResolver().openOutputStream(dbFile.getUri(), "w");
                                }
                                if (os != null) {
                                    os.write(bytes);
                                    os.flush();
                                    os.close();
                                    savedPath = "📁 " + prefs.getString("selected_folder_name", "Carpeta") + "/" + filename;
                                }
                            }
                        }
                    } catch (Exception treeErr) {
                        // Continuar a copia pública
                    }
                }

                // 2. Guardar SIEMPRE en la carpeta pública Descargas (Downloads) del teléfono
                try {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (downloadsDir != null) {
                        if (!downloadsDir.exists()) downloadsDir.mkdirs();
                        File pubFile = new File(downloadsDir, filename);
                        FileOutputStream pubFos = new FileOutputStream(pubFile);
                        pubFos.write(bytes);
                        pubFos.flush();
                        pubFos.close();
                        if (savedPath.isEmpty()) {
                            savedPath = "📁 Descargas/" + filename;
                        }
                    }
                } catch (Exception pubErr) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentValues cv = new ContentValues();
                            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                            cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                            Uri pubUri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                            if (pubUri != null) {
                                OutputStream pubOs = context.getContentResolver().openOutputStream(pubUri, "wt");
                                if (pubOs != null) {
                                    pubOs.write(bytes);
                                    pubOs.flush();
                                    pubOs.close();
                                    if (savedPath.isEmpty()) {
                                        savedPath = "📁 Descargas/" + filename;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // 3. Guardar SIEMPRE una copia de respaldo en almacenamiento de la app
                try {
                    File fallbackFile = new File(context.getExternalFilesDir(null), filename);
                    FileOutputStream fos = new FileOutputStream(fallbackFile);
                    fos.write(bytes);
                    fos.flush();
                    fos.close();
                    if (savedPath.isEmpty()) {
                        savedPath = fallbackFile.getAbsolutePath();
                    }
                } catch (Exception ignored) {}

                return savedPath;
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }

        @JavascriptInterface
        public String loadDbFile(String filename) {
            try {
                SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
                String treeUriStr = prefs.getString("selected_tree_uri", null);

                // 1. Cargar desde la carpeta elegida por el usuario
                if (treeUriStr != null && !treeUriStr.isEmpty()) {
                    try {
                        Uri treeUri = Uri.parse(treeUriStr);
                        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
                        if (pickedDir != null && pickedDir.exists()) {
                            DocumentFile dbFile = pickedDir.findFile(filename);
                            if (dbFile != null && dbFile.exists() && dbFile.length() > 0) {
                                InputStream is = context.getContentResolver().openInputStream(dbFile.getUri());
                                if (is != null) {
                                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                                    int nRead;
                                    byte[] data = new byte[16384];
                                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                                        buffer.write(data, 0, nRead);
                                    }
                                    buffer.flush();
                                    is.close();
                                    return Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP);
                                }
                            }
                        }
                    } catch (Exception treeErr) {
                        // Continuar
                    }
                }

                // 2. Cargar desde la carpeta pública Descargas
                try {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File pubFile = new File(downloadsDir, filename);
                    if (pubFile.exists() && pubFile.length() > 0) {
                        FileInputStream fis = new FileInputStream(pubFile);
                        byte[] bytes = new byte[(int) pubFile.length()];
                        fis.read(bytes);
                        fis.close();
                        return Base64.encodeToString(bytes, Base64.NO_WRAP);
                    }
                } catch (Exception ignored) {}

                // 3. Fallback: cargar desde getExternalFilesDir
                File fallbackFile = new File(context.getExternalFilesDir(null), filename);
                if (fallbackFile.exists() && fallbackFile.length() > 0) {
                    FileInputStream fis = new FileInputStream(fallbackFile);
                    byte[] bytes = new byte[(int) fallbackFile.length()];
                    fis.read(bytes);
                    fis.close();
                    return Base64.encodeToString(bytes, Base64.NO_WRAP);
                }

                return "";
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String getDbFilePath(String filename) {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            String folderName = prefs.getString("selected_folder_name", "");
            if (!folderName.isEmpty()) {
                return folderName + "/" + filename;
            }
            File pubFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            if (pubFile.exists()) {
                return "Descargas/" + filename;
            }
            File file = new File(context.getExternalFilesDir(null), filename);
            return file.exists() ? file.getAbsolutePath() : new File(context.getExternalFilesDir(null), filename).getAbsolutePath();
        }

        @JavascriptInterface
        public boolean hasDbFile(String filename) {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            String treeUriStr = prefs.getString("selected_tree_uri", null);
            if (treeUriStr != null && !treeUriStr.isEmpty()) {
                try {
                    Uri treeUri = Uri.parse(treeUriStr);
                    DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
                    if (pickedDir != null && pickedDir.exists()) {
                        DocumentFile dbFile = pickedDir.findFile(filename);
                        if (dbFile != null && dbFile.exists() && dbFile.length() > 0) {
                            return true;
                        }
                    }
                } catch (Exception e) {}
            }
            File pubFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            if (pubFile.exists() && pubFile.length() > 0) {
                return true;
            }
            File file = new File(context.getExternalFilesDir(null), filename);
            return file.exists() && file.length() > 0;
        }

        @JavascriptInterface
        public long getDbFileSize(String filename) {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            String treeUriStr = prefs.getString("selected_tree_uri", null);
            if (treeUriStr != null && !treeUriStr.isEmpty()) {
                try {
                    Uri treeUri = Uri.parse(treeUriStr);
                    DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
                    if (pickedDir != null && pickedDir.exists()) {
                        DocumentFile dbFile = pickedDir.findFile(filename);
                        if (dbFile != null && dbFile.exists()) {
                            return dbFile.length();
                        }
                    }
                } catch (Exception e) {}
            }
            File pubFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            if (pubFile.exists() && pubFile.isFile()) {
                return pubFile.length();
            }
            File file = new File(context.getExternalFilesDir(null), filename);
            return (file.exists() && file.isFile()) ? file.length() : 0;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FOLDER) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri treeUri = data.getData();
                try {
                    try {
                        final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                    } catch (Exception ignoredPerm) {}

                    DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                    String folderName = pickedDir != null && pickedDir.getName() != null ? pickedDir.getName() : "Carpeta seleccionada";

                    SharedPreferences prefs = getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
                    prefs.edit()
                         .putString("selected_tree_uri", treeUri.toString())
                         .putString("selected_folder_name", folderName)
                         .putBoolean("default_storage_chosen", false)
                         .apply();

                    boolean hasExistingDb = false;
                    if (pickedDir != null) {
                        DocumentFile dbFile = pickedDir.findFile("tarjetas.db");
                        if (dbFile != null && dbFile.exists() && dbFile.length() > 0) {
                            hasExistingDb = true;
                        }
                    }

                    final String folderJson = "{\"success\":true,\"uri\":\"" + escapeJson(treeUri.toString()) + "\",\"folderName\":\"" + escapeJson(folderName) + "\",\"hasExistingDb\":" + hasExistingDb + "}";
                    runOnUiThread(() -> {
                        if (getBridge() != null && getBridge().getWebView() != null) {
                            getBridge().getWebView().evaluateJavascript("if (window.onAndroidFolderPicked) window.onAndroidFolderPicked(" + folderJson + ");", null);
                        }
                    });
                } catch (Exception e) {
                    final String errJson = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                    runOnUiThread(() -> {
                        if (getBridge() != null && getBridge().getWebView() != null) {
                            getBridge().getWebView().evaluateJavascript("if (window.onAndroidFolderPicked) window.onAndroidFolderPicked(" + errJson + ");", null);
                        }
                    });
                }
            } else {
                runOnUiThread(() -> {
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        getBridge().getWebView().evaluateJavascript("if (window.onAndroidFolderPicked) window.onAndroidFolderPicked({\"success\":false,\"cancelled\":true});", null);
                    }
                });
            }
        } else if (requestCode == REQUEST_CODE_PICK_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                try {
                    final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    try {
                        getContentResolver().takePersistableUriPermission(fileUri, takeFlags);
                    } catch (Exception ignored) {}

                    InputStream is = getContentResolver().openInputStream(fileUri);
                    if (is != null) {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] dataArr = new byte[16384];
                        while ((nRead = is.read(dataArr, 0, dataArr.length)) != -1) {
                            buffer.write(dataArr, 0, nRead);
                        }
                        buffer.flush();
                        byte[] allBytes = buffer.toByteArray();
                        is.close();

                        String base64 = Base64.encodeToString(allBytes, Base64.NO_WRAP);
                        DocumentFile docFile = DocumentFile.fromSingleUri(this, fileUri);
                        String fileName = docFile != null && docFile.getName() != null ? docFile.getName() : "tarjetas.db";

                        final String fileJson = "{\"success\":true,\"fileName\":\"" + escapeJson(fileName) + "\",\"base64\":\"" + base64 + "\"}";
                        runOnUiThread(() -> {
                            if (getBridge() != null && getBridge().getWebView() != null) {
                                getBridge().getWebView().evaluateJavascript("if (window.onAndroidFilePicked) window.onAndroidFilePicked(" + fileJson + ");", null);
                            }
                        });
                    }
                } catch (Exception e) {
                    final String errJson = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                    runOnUiThread(() -> {
                        if (getBridge() != null && getBridge().getWebView() != null) {
                            getBridge().getWebView().evaluateJavascript("if (window.onAndroidFilePicked) window.onAndroidFilePicked(" + errJson + ");", null);
                        }
                    });
                }
            } else {
                runOnUiThread(() -> {
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        getBridge().getWebView().evaluateJavascript("if (window.onAndroidFilePicked) window.onAndroidFilePicked({\"success\":false,\"cancelled\":true});", null);
                    }
                });
            }
        } else if (requestCode == REQUEST_CODE_SAVE_FILE_AS) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingSaveBytes != null) {
                Uri destUri = data.getData();
                try {
                    OutputStream os = getContentResolver().openOutputStream(destUri, "wt");
                    if (os != null) {
                        os.write(pendingSaveBytes);
                        os.flush();
                        os.close();
                    }
                    DocumentFile doc = DocumentFile.fromSingleUri(this, destUri);
                    String name = doc != null && doc.getName() != null ? doc.getName() : "tarjetas.db";

                    final String resJson = "{\"success\":true,\"fileName\":\"" + escapeJson(name) + "\"}";
                    runOnUiThread(() -> {
                        if (getBridge() != null && getBridge().getWebView() != null) {
                            getBridge().getWebView().evaluateJavascript("if (window.onAndroidFileSaved) window.onAndroidFileSaved(" + resJson + ");", null);
                        }
                    });
                } catch (Exception e) {
                    final String errJson = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                    runOnUiThread(() -> {
                        if (getBridge() != null && getBridge().getWebView() != null) {
                            getBridge().getWebView().evaluateJavascript("if (window.onAndroidFileSaved) window.onAndroidFileSaved(" + errJson + ");", null);
                        }
                    });
                } finally {
                    pendingSaveBytes = null;
                }
            } else {
                pendingSaveBytes = null;
                runOnUiThread(() -> {
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        getBridge().getWebView().evaluateJavascript("if (window.onAndroidFileSaved) window.onAndroidFileSaved({\"success\":false,\"cancelled\":true});", null);
                    }
                });
            }
        }
    }
}
