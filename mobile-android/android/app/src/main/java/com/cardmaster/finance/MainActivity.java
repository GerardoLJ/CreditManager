package com.cardmaster.finance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                              | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
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
            String fileUri = prefs.getString("selected_file_uri", null);
            String treeUri = prefs.getString("selected_tree_uri", null);
            boolean defaultChosen = prefs.getBoolean("default_storage_chosen", false);
            return (fileUri != null && !fileUri.isEmpty()) || (treeUri != null && !treeUri.isEmpty()) || defaultChosen;
        }

        @JavascriptInterface
        public void setDefaultStorageChosen(boolean chosen) {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("default_storage_chosen", chosen).apply();
        }

        @JavascriptInterface
        public String getStorageInfo() {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            String fileUri = prefs.getString("selected_file_uri", "");
            String fileName = prefs.getString("selected_file_name", "");
            String treeUri = prefs.getString("selected_tree_uri", "");
            String folderName = prefs.getString("selected_folder_name", "");
            boolean defaultChosen = prefs.getBoolean("default_storage_chosen", false);
            boolean configured = isStorageConfigured();
            boolean hasDb = hasDbFile("tarjetas.db");
            long sizeBytes = getDbFileSize("tarjetas.db");

            String folderDisplay = "";
            String display = "";

            if (!fileName.isEmpty()) {
                folderDisplay = fileName;
                display = "📄 " + fileName;
            } else if (!folderName.isEmpty()) {
                folderDisplay = folderName;
                display = "📁 " + folderName + "/tarjetas.db";
            } else if (defaultChosen) {
                folderDisplay = "Almacenamiento Interno";
                display = "💾 Almacenamiento interno de la app";
            } else {
                folderDisplay = "Sin configurar";
                display = "⚠️ Sin vincular (toca Guardar o Cargar)";
            }

            if (hasDb && !configured) {
                configured = true;
            }

            return "{\"configured\":" + configured + ",\"folderName\":\"" + escapeJson(folderDisplay) + "\",\"pathDisplay\":\"" + escapeJson(display) + "\",\"hasDb\":" + hasDb + ",\"sizeBytes\":" + sizeBytes + "}";
        }

        @JavascriptInterface
        public void resetStorageLocation() {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            prefs.edit()
                 .remove("selected_file_uri")
                 .remove("selected_file_name")
                 .remove("selected_tree_uri")
                 .remove("selected_folder_name")
                 .remove("default_storage_chosen")
                 .apply();
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
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                              | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
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
                if (bytes == null || bytes.length == 0) {
                    return "ERROR: Datos vacíos";
                }

                // 1. SIEMPRE guardar en almacenamiento interno de la app como respaldo permanente
                try {
                    File internalFile = new File(context.getExternalFilesDir(null), filename);
                    FileOutputStream fos = new FileOutputStream(internalFile);
                    fos.write(bytes);
                    fos.flush();
                    fos.close();
                } catch (Exception ignored) {}

                SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
                String fileUriStr = prefs.getString("selected_file_uri", null);
                String treeUriStr = prefs.getString("selected_tree_uri", null);

                // 2. Guardar en el ARCHIVO físico vinculado
                if (fileUriStr != null && !fileUriStr.isEmpty()) {
                    try {
                        Uri fileUri = Uri.parse(fileUriStr);
                        OutputStream os = null;
                        try {
                            os = context.getContentResolver().openOutputStream(fileUri, "wt");
                        } catch (Exception eWt) {
                            os = context.getContentResolver().openOutputStream(fileUri, "w");
                        }
                        if (os != null) {
                            os.write(bytes);
                            os.flush();
                            os.close();
                            String fileName = prefs.getString("selected_file_name", filename != null ? filename : "tarjetas.db");
                            return "📄 " + fileName;
                        }
                    } catch (Exception fileErr) {
                        // Permiso denegado o archivo no accesible
                    }
                }

                // 3. Guardar en la CARPETA física vinculada
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
                                    return "📁 " + prefs.getString("selected_folder_name", "Carpeta") + "/" + filename;
                                }
                            }
                        }
                    } catch (Exception treeErr) {}
                }

                // Si no había ubicación vinculada o falló el permiso del archivo vinculado
                return "ERROR:NEED_SAVE_AS";
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }

        @JavascriptInterface
        public String loadDbFile(String filename) {
            try {
                SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
                String fileUriStr = prefs.getString("selected_file_uri", null);
                String treeUriStr = prefs.getString("selected_tree_uri", null);

                // 1. Cargar desde el archivo físico vinculado
                if (fileUriStr != null && !fileUriStr.isEmpty()) {
                    try {
                        Uri fileUri = Uri.parse(fileUriStr);
                        InputStream is = context.getContentResolver().openInputStream(fileUri);
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
                    } catch (Exception fileErr) {}
                }

                // 2. Cargar desde la carpeta física vinculada
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
                    } catch (Exception treeErr) {}
                }

                // 3. Fallback: cargar desde almacenamiento interno de la app
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
            String fileName = prefs.getString("selected_file_name", "");
            if (!fileName.isEmpty()) {
                return "📄 " + fileName;
            }
            String folderName = prefs.getString("selected_folder_name", "");
            if (!folderName.isEmpty()) {
                return "📁 " + folderName + "/" + filename;
            }
            File file = new File(context.getExternalFilesDir(null), filename);
            return file.exists() ? file.getAbsolutePath() : filename;
        }

        @JavascriptInterface
        public boolean hasDbFile(String filename) {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            String fileUriStr = prefs.getString("selected_file_uri", null);
            if (fileUriStr != null && !fileUriStr.isEmpty()) {
                try {
                    Uri fileUri = Uri.parse(fileUriStr);
                    DocumentFile docFile = DocumentFile.fromSingleUri(context, fileUri);
                    if (docFile != null && docFile.exists() && docFile.length() > 0) {
                        return true;
                    }
                } catch (Exception e) {}
            }

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

            File file = new File(context.getExternalFilesDir(null), filename);
            return file.exists() && file.length() > 0;
        }

        @JavascriptInterface
        public long getDbFileSize(String filename) {
            SharedPreferences prefs = context.getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
            String fileUriStr = prefs.getString("selected_file_uri", null);
            if (fileUriStr != null && !fileUriStr.isEmpty()) {
                try {
                    Uri fileUri = Uri.parse(fileUriStr);
                    DocumentFile docFile = DocumentFile.fromSingleUri(context, fileUri);
                    if (docFile != null && docFile.exists()) {
                        return docFile.length();
                    }
                } catch (Exception e) {}
            }

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
                    int flags = data.getFlags();
                    int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    if (takeFlags != 0) {
                        try {
                            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                        } catch (Exception e1) {
                            try {
                                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            } catch (Exception ignoredPerm) {}
                        }
                    }

                    DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                    String folderName = pickedDir != null && pickedDir.getName() != null ? pickedDir.getName() : "Carpeta seleccionada";

                    SharedPreferences prefs = getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
                    prefs.edit()
                         .putString("selected_tree_uri", treeUri.toString())
                         .putString("selected_folder_name", folderName)
                         .remove("selected_file_uri")
                         .remove("selected_file_name")
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
                    int flags = data.getFlags();
                    int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    if (takeFlags != 0) {
                        try {
                            getContentResolver().takePersistableUriPermission(fileUri, takeFlags);
                        } catch (SecurityException se1) {
                            try {
                                getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception ignored) {}
                        }
                    }

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

                        // Guardar siempre copia interna garantizada en almacenamiento de la app
                        try {
                            File internalFile = new File(getExternalFilesDir(null), "tarjetas.db");
                            FileOutputStream fos = new FileOutputStream(internalFile);
                            fos.write(allBytes);
                            fos.flush();
                            fos.close();
                        } catch (Exception ignored) {}

                        DocumentFile docFile = DocumentFile.fromSingleUri(this, fileUri);
                        String fileName = docFile != null && docFile.getName() != null ? docFile.getName() : "tarjetas.db";

                        SharedPreferences prefs = getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
                        prefs.edit()
                             .putString("selected_file_uri", fileUri.toString())
                             .putString("selected_file_name", fileName)
                             .remove("selected_tree_uri")
                             .remove("selected_folder_name")
                             .putBoolean("default_storage_chosen", false)
                             .apply();

                        // Enviar respuesta liviana para evitar límites de tamaño en evaluateJavascript
                        final String fileJson = "{\"success\":true,\"fileName\":\"" + escapeJson(fileName) + "\"}";
                        runOnUiThread(() -> {
                            if (getBridge() != null && getBridge().getWebView() != null) {
                                getBridge().getWebView().evaluateJavascript("if (window.onAndroidFilePicked) window.onAndroidFilePicked(" + fileJson + ");", null);
                            }
                        });
                    } else {
                        throw new Exception("No se pudo abrir el archivo seleccionado");
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
                    int flags = data.getFlags();
                    int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    if (takeFlags != 0) {
                        try {
                            getContentResolver().takePersistableUriPermission(destUri, takeFlags);
                        } catch (SecurityException se1) {
                            try {
                                getContentResolver().takePersistableUriPermission(destUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            } catch (SecurityException se2) {
                                try {
                                    getContentResolver().takePersistableUriPermission(destUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                } catch (Exception ignored) {}
                            }
                        }
                    }

                    OutputStream os = null;
                    try {
                        os = getContentResolver().openOutputStream(destUri, "wt");
                    } catch (Exception eWt) {
                        os = getContentResolver().openOutputStream(destUri, "w");
                    }
                    if (os != null) {
                        os.write(pendingSaveBytes);
                        os.flush();
                        os.close();
                    }

                    // Guardar copia interna garantizada
                    try {
                        File internalFile = new File(getExternalFilesDir(null), "tarjetas.db");
                        FileOutputStream fos = new FileOutputStream(internalFile);
                        fos.write(pendingSaveBytes);
                        fos.flush();
                        fos.close();
                    } catch (Exception ignored) {}

                    DocumentFile doc = DocumentFile.fromSingleUri(this, destUri);
                    String name = doc != null && doc.getName() != null ? doc.getName() : "tarjetas.db";

                    SharedPreferences prefs = getSharedPreferences("CardMasterStorage", Context.MODE_PRIVATE);
                    prefs.edit()
                         .putString("selected_file_uri", destUri.toString())
                         .putString("selected_file_name", name)
                         .remove("selected_tree_uri")
                         .remove("selected_folder_name")
                         .putBoolean("default_storage_chosen", false)
                         .apply();

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
