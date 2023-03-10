package com.google.android.pixelnfc.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import android.os.SystemProperties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Formatter;

/**
 * Used to query if the device is a Japanese release or not
 */
public class DeviceInfoContentProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    private boolean isJapanSku() {
        String[] validSkus = {
            // Taken from reverse engineered PixelNfc app
            "G020N", // Pixel 4
            "G020Q", // Pixel 4 XL
            "G025M", // Pixel 4a
            "G025H", // Pixel 4a 5G
            "G5NZ6", // Pixel 5
            "G4S1M", // Pixel 5a
            "GR1YH", // Pixel 6
            "GF5KQ", // Pixel 6 Pro
            "GPQ72", // ??? Unreleased Pixel
            "GB17L", // Pixel 6a
            "GO3Z5", // Pixel 7
            "GFE4J", // Pixel 7 Pro
            "G82U8", // ??? Unreleased Pixel
            "G0B96", // ??? Unreleased Pixel
        };
        
	    String systemSku = SystemProperties.get("ro.boot.hardware.sku");
	    Log.i(this.getClass().getName(), "SKU: " + systemSku);

	    return Arrays.asList(validSkus).contains(systemSku);
    }

    /**
     While this app doesn't provide information that can't be fetched through other means, we are keeping this behavior for future use.
     This function ensures only specific apps can use the ContentProvider
     */
    private boolean validateCaller(String packageName) {
        String[] allowedCallers = {
            "BE51DBF4FEC89BD32846457B13B7300876AF5594D2874DEE026904965AE4A6CB"
        };
        try {
            for(Signature sig : getContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo.getApkContentsSigners()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                Formatter formatter = new Formatter();
                byte[] shasum = digest.digest(sig.toByteArray());
                for(byte b : shasum) {
                    formatter.format("%02X", b);
                }
                boolean validCaller = Arrays.asList(allowedCallers).contains(formatter.toString());
                if(!validCaller) {
                    Log.w(this.getClass().getName(), "Unable to verify caller");
                } else {
                    Log.w(this.getClass().getName(), "Valid caller");
                }
                return validCaller;
            }
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "Failed to verify integrity of caller: " + e);
            return false;
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        UriMatcher matcher = new UriMatcher(-1);
        matcher.addURI("com.google.android.pixelnfc.provider.DeviceInfoContentProvider", "isJapanSku", 1);
        if(matcher.match(uri) == 1) {
            if(this.isJapanSku() && this.validateCaller(getCallingPackage())) {
                MatrixCursor cursor = new MatrixCursor(new String[] {
                        "is_japan_sku"
                });
                cursor.newRow().add(this.isJapanSku() ? 1 : 0);
                return (Cursor)cursor;
            }
            return null;
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        throw new UnsupportedOperationException("Not supported");
    }
}
