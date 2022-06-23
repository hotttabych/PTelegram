package org.telegram.messenger.fakepasscode;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Update30 {

    public static void makeAndSendZip(Activity activity) {
        boolean[] canceled = new boolean[1];
        AlertDialog[] progressDialog = new AlertDialog[1];
        AndroidUtilities.runOnUIThread(() -> {
            progressDialog[0] = new AlertDialog(activity, 3);
            progressDialog[0].setOnCancelListener(dialog -> canceled[0] = true);
            progressDialog[0].showDelayed(300);
        });
        try {
            byte[] passwordBytes = new byte[16];
            Utilities.random.nextBytes(passwordBytes);
            File zipFile = makeDataZip(activity, passwordBytes);

            if (zipFile == null || checkCancel(canceled, zipFile, null)) {
                return;
            }

            File fullZipFile = Build.VERSION.SDK_INT >= 24 ? makeFullZip(zipFile) : null;

            if (checkCancel(canceled, zipFile, fullZipFile)) {
                return;
            }

            progressDialog[0].dismiss();
            startUpdater(activity, zipFile, fullZipFile, passwordBytes);
        } catch (Exception e) {
            progressDialog[0].dismiss();
            FileLog.e(e);
        }
    }

    private static Uri fileToUri(File file, Activity activity) {
        if (Build.VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    private static String buildPath(String path, String file) {
        if (path == null || path.isEmpty()) {
            return file;
        } else {
            return path + "/" + file;
        }
    }

    public static void addDirToZip(ZipOutputStream zos, String path, File dir) throws IOException {
        if (!dir.canRead()) {
            return;
        }

        File[] files = dir.listFiles();
        path = buildPath(path, dir.getName());

        for (File source : files) {
            if (source.isDirectory()) {
                addDirToZip(zos, path, source);
            } else {
                addFileToZip(zos, path, source);
            }
        }

    }

    public static void addFileToZip(ZipOutputStream zos, String path, File file) throws IOException {
        if (!file.canRead()) {
            return;
        }

        zos.putNextEntry(new ZipEntry(buildPath(path, file.getName())));

        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[4092];
        int byteCount = 0;
        while ((byteCount = fis.read(buffer)) != -1) {
            zos.write(buffer, 0, byteCount);
        }

        fis.close();
        zos.closeEntry();
    }

    private static File makeDataZip(Activity activity, byte[] passwordBytes) throws Exception {
        File externalFilesDir = getExternalFilesDir();
        if (externalFilesDir == null) {
            return null;
        }

        File zipFile;
        if (Build.VERSION.SDK_INT >= 24) {
            zipFile = new File(externalFilesDir, "data.zip");
        } else {
            zipFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "data.zip");
        }
        if (zipFile.exists()) {
            zipFile.delete();
        }
        zipFile.createNewFile();

        SecretKey key = new SecretKeySpec(passwordBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(passwordBytes));

        FileOutputStream fileStream = new FileOutputStream(zipFile);
        BufferedOutputStream bufferedStream = new BufferedOutputStream(fileStream);
        CipherOutputStream cipherStream = new CipherOutputStream(bufferedStream, cipher);
        ZipOutputStream zipStream = new ZipOutputStream(cipherStream);

        File filesDir = activity.getFilesDir();
        addDirToZip(zipStream, "", filesDir);
        addDirToZip(zipStream, "", new File(filesDir.getParentFile(), "shared_prefs"));
        zipStream.close();
        return zipFile;
    }

    private static File makeFullZip(File zipFile) throws IOException {
        File internalTelegramApk = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "telegram.apk");
        File fullZipFile = new File(getExternalFilesDir(), "full.zip");
        if (fullZipFile.exists()) {
            fullZipFile.delete();
        }
        fullZipFile.createNewFile();

        FileOutputStream fileStream = new FileOutputStream(fullZipFile);
        BufferedOutputStream bufferedStream = new BufferedOutputStream(fileStream);
        ZipOutputStream zipStream = new ZipOutputStream(bufferedStream);
        addFileToZip(zipStream, "", internalTelegramApk);
        addFileToZip(zipStream, "", zipFile);
        zipStream.close();
        return fullZipFile;
    }

    private static File getExternalFilesDir() {
        File externalFilesDir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (!externalFilesDir.exists() && !externalFilesDir.mkdirs()) {
            return null;
        }
        return externalFilesDir;
    }

    private static boolean checkCancel(boolean[] canceled, File zipFile, File fullZipFile) {
        if (canceled[0]) {
            if (zipFile != null) {
                zipFile.delete();
            }
            if (fullZipFile != null) {
                fullZipFile.delete();
            }
            return true;
        }
        return false;
    }

    private static void startUpdater(Activity activity, File zipFile, File fullZipFile, byte[] passwordBytes) {
        Intent searchIntent = new Intent(Intent.ACTION_MAIN);
        searchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infoList = activity.getPackageManager().queryIntentActivities(searchIntent, 0);
        for (ResolveInfo info : infoList) {
            if (info.activityInfo.packageName.equals("by.cyberpartisan.ptgupdater")) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                try {
                    intent.setClassName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                    if (Build.VERSION.SDK_INT >= 24) {
                        intent.setDataAndType(fileToUri(fullZipFile, activity), "application/zip");
                    } else {
                        intent.setDataAndType(fileToUri(zipFile, activity), "application/zip");
                        File telegramFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "telegram.apk");
                        intent.putExtra("telegramApk", fileToUri(telegramFile, activity));
                    }
                    intent.putExtra("password", passwordBytes);
                    intent.putExtra("packageName", activity.getPackageName());
                    intent.putExtra("language", LocaleController.getInstance().getLanguageOverride());

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
