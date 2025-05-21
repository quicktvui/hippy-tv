package com.quicktvui.hippyext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.annotation.HippyMethod;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.modules.javascriptmodules.EventDispatcher;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.ReflectUtils;

@HippyNativeModule(name = UsbManagerModule.CLASS_NAME)
public class UsbManagerModule extends HippyNativeModuleBase {

  public static final String CLASS_NAME = "UsbManagerModule";
  private static final String TAG = CLASS_NAME;
  private static final String BASE64_HEAD = "data:image/png;base64,";
  private static final String FILE_PROVIDER_SUFFIX = ".provider";
  private static final String FILE_EXT_NAME_APK = ".apk";
  private static final String URI_SCHEME_TYPE_FILE = "file://";
  private static final String DATA_SCHEME_TYPE_FILE = "file";
  private static final String MIME_TYPE_APPLICATION = "application/vnd.android.package-archive";

  private static final String FUNC_GET_VOLUME_LIST = "getVolumeList";
  private static final String FUNC_GET_USER_LABEL = "getUserLabel";
  private static final String FUNC_GET_STATE = "getState";
  private static final String FUNC_IS_REMOVABLE = "isRemovable";
  private static final String FUNC_GET_PATH_FILE = "getPathFile";

  private static final String DEVICE_USED_STORAGE = "used_storage";
  private static final String DEVICE_ALL_STORAGE = "all_storage";
  private static final String DEVICE_PRODUCT_NAME = "productName";
  private static final String DEVICE_FILE_PATH = "filePath";

  private static final String KEY_NAME = "name";
  private static final String KEY_PATH = "path";
  private static final String KEY_LAST_MODIFIED = "lastModified";
  private static final String KEY_IS_DIRECTORY = "isDirectory";
  private static final String KEY_PACKAGE_NAME = "packageName";
  private static final String KEY_INSTALLED = "installed";
  private static final String KEY_LABEL = "label";
  private static final String KEY_ICON = "icon";
  private static final String KEY_BANNER = "banner";
  private static final String KEY_HAS_BANNER = "hasBanner";
  private static final float GB_BASE_CNT = 1024 * 1024 * 1024;

  private UsbReceiver mUsbReceiver;
  private Context mAppContext;
  private final StorageManager mStorageManager;
  private final PackageManager mPackageManager;
  private HashMap<String, String> mNameMap = new HashMap<String,String>();

  public UsbManagerModule(HippyEngineContext context) {
    super(context);
    mAppContext = context.getGlobalConfigs().getContext();
    mStorageManager = (StorageManager) mAppContext.getSystemService(Context.STORAGE_SERVICE);
    mPackageManager = mAppContext.getPackageManager();
    registerReceiver();
  }

  @HippyMethod(name = "getApkIcon")
  public void getApkIcon(String apkPath, Promise promise) {
    LogUtils.d(TAG, "getApkIcon(), path="+apkPath);
    HippyMap retMap = new HippyMap();
    if (apkPath != null) {
      try {
        File file = new File(apkPath);
        if (isNeededFile(file) && file.isFile()) {
          PackageInfo info = mPackageManager.getPackageArchiveInfo(file.getPath(),PackageManager.GET_META_DATA);
          if (info != null) {
            String packageName = info.packageName;
            boolean isInstalled = isInstalled(packageName);
            ApplicationInfo queryInfo = info.applicationInfo;
            if (isInstalled) {
              queryInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            } else {
              queryInfo.publicSourceDir = file.getPath();
            }
            Drawable banner = null;
            Drawable icon = null;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
              banner = mPackageManager.getApplicationBanner(queryInfo);
            }
            if (banner == null) {
              icon = mPackageManager.getApplicationIcon(queryInfo);
            }
            retMap.pushString(KEY_PATH, file.getPath());
            retMap.pushString(KEY_ICON, drawableToBase64(icon));
            retMap.pushString(KEY_BANNER, drawableToBase64(banner));
          } else {
            retMap.pushString(KEY_PATH, file.getPath());
            retMap.pushString(KEY_ICON, null);
            retMap.pushString(KEY_BANNER, null);
          }
        }
      } catch (Exception e) {
        LogUtils.d(TAG, "getApkIcon(), ex=="+e.toString());
      }
    }
    if(promise != null) {
      promise.resolve(retMap);
    }
  }

  @HippyMethod(name = "getFileList")
  public void getFileList(String path, Promise promise) {
    LogUtils.d(TAG, "getFileList(), path="+path);
    HippyMap retMap = new HippyMap();
    if (path != null) {
      try {
        File rootFile = new File(path);
        if (rootFile.exists()) {
          for (File file : rootFile.listFiles()) {
            if (isNeededFile(file)) {
              HippyMap entryMap = new HippyMap();
              entryMap.pushString(KEY_NAME, file.getName());
              entryMap.pushString(KEY_PATH, file.getPath());
              entryMap.pushLong(KEY_LAST_MODIFIED, file.lastModified());
              entryMap.pushBoolean(KEY_IS_DIRECTORY, file.isDirectory());
              if (file.isFile()) {
                try {
                  PackageInfo info = mPackageManager.getPackageArchiveInfo(file.getPath(),PackageManager.GET_META_DATA);
                  if (info != null) {
                    String packageName = info.packageName;
                    boolean isInstalled = isInstalled(packageName);
                    entryMap.pushString(KEY_PACKAGE_NAME, packageName);
                    entryMap.pushBoolean(KEY_INSTALLED, isInstalled);

                    ApplicationInfo queryInfo = info.applicationInfo;
                    if (isInstalled) {
                      queryInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                    } else {
                      queryInfo.publicSourceDir = file.getPath();
                    }
                    String label = mPackageManager.getApplicationLabel(queryInfo).toString();
                    LogUtils.d(TAG, "Apk Label="+label + ", packageName="+packageName + ", isInstalled="+isInstalled);
                    Drawable icon = null; // mPackageManager.getApplicationIcon(queryInfo);
                    Drawable banner = null;
                    boolean hasBanner = false;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                      banner = mPackageManager.getApplicationBanner(queryInfo);
                    }
                    if (banner != null) {
                      hasBanner = true;
                    }
                    entryMap.pushString(KEY_LABEL, label);
                    entryMap.pushString(KEY_ICON, null);
                    entryMap.pushString(KEY_BANNER, null);
                    entryMap.pushBoolean(KEY_HAS_BANNER, hasBanner);
                  } else {
                    entryMap.pushString(KEY_PACKAGE_NAME, null);
                    entryMap.pushBoolean(KEY_INSTALLED, false);
                    entryMap.pushString(KEY_LABEL, null);
                    entryMap.pushString(KEY_ICON, null);
                    entryMap.pushString(KEY_BANNER, null);
                    entryMap.pushBoolean(KEY_HAS_BANNER, false);
                  }
                } catch (Exception e) {
                  entryMap.pushString(KEY_PACKAGE_NAME, null);
                  entryMap.pushBoolean(KEY_INSTALLED, false);
                  entryMap.pushString(KEY_LABEL, null);
                  entryMap.pushString(KEY_ICON, null);
                  entryMap.pushString(KEY_BANNER, null);
                  entryMap.pushBoolean(KEY_HAS_BANNER, false);
                  LogUtils.d(TAG, "getFileList(), e="+e.toString());
                }
              }
              retMap.pushMap(file.getPath(), entryMap);
            }
          }
        }
      } catch (Exception e) {
        LogUtils.d(TAG, "getFileList(), ex=="+e.toString());
      }
    }
    if(promise != null) {
      promise.resolve(retMap);
    }
  }

  private String drawableToBase64(Drawable drawable) {
    String result = null;
    if (drawable != null){
      try {
        Bitmap bitmap = null;
        if(drawable instanceof BitmapDrawable) {
          bitmap = ((BitmapDrawable) drawable).getBitmap();
        }
        if (bitmap == null) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (drawable instanceof AdaptiveIconDrawable) {
              AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) drawable;
              bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
              Canvas canvas = new Canvas(bitmap);
              adaptiveIconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
              adaptiveIconDrawable.draw(canvas);
            }
          }
        }
        if (bitmap != null) {
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
          byte[] byteArray = byteArrayOutputStream.toByteArray();
          result = BASE64_HEAD + Base64.encodeToString(byteArray, Base64.DEFAULT);
//          LogUtils.d(TAG, "drawableToBase64(), result="+result);
        }
      } catch (Exception e) {
        LogUtils.d(TAG, "drawableToBase64(), e="+e.getMessage());
      }
    }
    return result;
  }

  @HippyMethod(name = "isInstalled")
  public void isIntalled(String apkPath, Promise promise) {
    boolean ret = false;
    if (apkPath != null && apkPath.length() > 0) {
      PackageInfo info = mPackageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA);
      if (info != null) {
        String packageName = info.packageName;
        ret = isInstalled(packageName);
      }
    }
    if(promise != null) {
      promise.resolve(ret);
    }
  }

  private boolean isInstalled(String packageName) {
    if (packageName != null) {
      try {
        PackageInfo info = mPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        if (info != null) {
          return true;
        }
      } catch (Exception e) {
        LogUtils.d(TAG, "isInstalled(), e="+e.toString());
      }
    }
    return false;
  }

  private boolean isNeededFile(File file) {
    if (file != null && file.exists() && !file.isHidden()) {
      if (file.isDirectory()) {
        return true;
      } else {
        if (file.getName().endsWith(FILE_EXT_NAME_APK)) {
          return true;
        }
      }
    }
    return false;
  }

  @HippyMethod(name = "installApk")
  public void installApk(String apkPath) {
    LogUtils.d(TAG, "installApk(), apkPath="+apkPath);
    installApkBySystem(apkPath);
  }

  private void installApkBySystem(String apkPath) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      Uri contentUri = null;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        File file = new File(apkPath);
        String authority = mAppContext.getApplicationContext().getPackageName() + FILE_PROVIDER_SUFFIX;
        contentUri = FileProvider.getUriForFile(mAppContext, authority, file);
      } else {
        contentUri = Uri.parse(URI_SCHEME_TYPE_FILE + apkPath);
      }
      intent.setDataAndType(contentUri, MIME_TYPE_APPLICATION);
      mAppContext.startActivity(intent);
    } catch (Exception e) {
      LogUtils.d(TAG, "installApkBySystem(), e="+e.toString());
    }
  }

  @HippyMethod(name = "getDeviceList")
  public void getDeviceList(Promise promise) {
    LogUtils.d(TAG, "getDeviceList()");
    List<UsbDeviceInfo> infoList = getDeviceInfoList();
    HippyMap retMap = new HippyMap();
    try {
      for (UsbDeviceInfo info : infoList) {
        LogUtils.d(TAG, "getDeviceList: " + info.toString());
        if (info.isRemovable && info.state.equals(Environment.MEDIA_MOUNTED)) {
          float usedSpace = (float)(info.pathFile.getTotalSpace() - info.pathFile.getUsableSpace());
          float totalSpace = (float)info.pathFile.getTotalSpace();
          HippyMap entryMap = new HippyMap();
          entryMap.pushString(DEVICE_PRODUCT_NAME, info.label);
          entryMap.pushString(DEVICE_FILE_PATH, info.pathFile.getPath());
          entryMap.pushString(DEVICE_USED_STORAGE, String.format("%.2f", usedSpace/GB_BASE_CNT));
          entryMap.pushString(DEVICE_ALL_STORAGE, String.format("%.2f", totalSpace/GB_BASE_CNT));
          retMap.pushMap(info.pathFile.getPath(), entryMap);
        }
      }
    } catch (Exception e) {
      LogUtils.d(TAG, "getDeviceList: " + e.toString());
    }
    if(promise != null) {
      promise.resolve(retMap);
    }
  }

  private List<UsbDeviceInfo> getDeviceInfoList() {
    List<UsbDeviceInfo> infoList = new ArrayList();
    try {
      Object[] volumeArray;
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        volumeArray = mStorageManager.getStorageVolumes().toArray();
      } else {
        volumeArray = (Object[]) ReflectUtils.reflect(mStorageManager).method(FUNC_GET_VOLUME_LIST).get();
      }
      for (Object obj: volumeArray) {
        ReflectUtils rf = ReflectUtils.reflect(obj);
        UsbDeviceInfo info = new UsbDeviceInfo((String)rf.method(FUNC_GET_USER_LABEL).get(),
                                              (String)rf.method(FUNC_GET_STATE).get(),
                                              (Boolean)rf.method(FUNC_IS_REMOVABLE).get(),
                                              (File)rf.method(FUNC_GET_PATH_FILE).get());
        infoList.add(info);
        mNameMap.put(info.pathFile.getPath(), info.label);
      }
    } catch (Exception e) {
      LogUtils.d(TAG, "getDeviceInfoList, e="+e.toString());
    }
    return infoList;
  }

  @Override
  public void destroy() {
    super.destroy();
    unregisterReceiver();
  }

  @Override
  public void handleAddListener(String name) {
    registerReceiver();
  }

  @Override
  public void handleRemoveListener(String name) {
    unregisterReceiver();
  }

  private void registerReceiver() {
    LogUtils.d(TAG, "registerReceiver()");
    if (mUsbReceiver == null) {
      mUsbReceiver = new UsbReceiver();
    }
    try {
      IntentFilter usbFilter = new IntentFilter();
      usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
      usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
      mAppContext.registerReceiver(mUsbReceiver, usbFilter);

      IntentFilter mountFilter = new IntentFilter();
      mountFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
      mountFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
      mountFilter.addDataScheme(DATA_SCHEME_TYPE_FILE);
      mAppContext.registerReceiver(mUsbReceiver, mountFilter);
    } catch (Throwable e) {
      LogUtils.d(TAG, "registerReceiver ex= " + e.toString());
    }
  }

  private void unregisterReceiver() {
    try {
      if (mUsbReceiver != null) {
        mContext.getGlobalConfigs().getContext().unregisterReceiver(mUsbReceiver);
        mUsbReceiver = null;
      }
    } catch (Throwable e) {
      LogUtils.d(TAG, "unregisterReceiver ex= " + e.toString());
    }
  }

  private class UsbDeviceInfo {
    private String label;
    private String state;
    private boolean isRemovable;
    private File pathFile;
    UsbDeviceInfo(String label, String state, boolean isRemovable, File pathFile) {
      this.label = label;
      this.state = state;
      this.isRemovable = isRemovable;
      this.pathFile = pathFile;
    }
    public String toString() {
      return "UsbDeviceInfo[ label="+ label +", state="+state + ", isRemovable="+isRemovable + ", file="+pathFile + " ]";
    }
  }

  private class UsbReceiver extends BroadcastReceiver {
    private final String USB_DEVICE_EVENT_NAME = "onUsbDeviceChanged";
    private final String EVENT_ATTACHED = "attached";
    private final String EVENT_DETACHED = "detached";
    private final String EVENT_MOUNTED = "mounted";
    private final String EVENT_UNMOUNTED = "unmounted";
    private final String EVENT_NAME = "eventName";

    @Override
    public void onReceive(Context context, Intent intent) {
      String path = null;
      String action = intent.getAction();
      LogUtils.d(TAG, "onReceive intent= " + intent.toString());
      if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
        sendEvent(EVENT_ATTACHED, getProductName(intent), null);
      } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
        String name = null;
        List<UsbDeviceInfo> list = getDeviceInfoList();
        for (UsbDeviceInfo info : list) {
          if (info.state.equals(Environment.MEDIA_EJECTING)) {
            name = info.label;
            path = info.pathFile.getPath();
          }
        }
        sendEvent(EVENT_DETACHED, name, path);
      } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
        path = intent.getData().getPath();
        sendEvent(EVENT_MOUNTED, getProductName(path), path);
      } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
        path = intent.getData().getPath();
        sendEvent(EVENT_UNMOUNTED, getProductName(path), path);
      }
    }

    private String getProductName(Intent intent) {
      if (intent != null) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
          if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return device.getProductName();
          } else {
            return device.getDeviceName();
          }
        }
      }
      return null;
    }

    private String getProductName(String path) {
      List<UsbDeviceInfo> list = getDeviceInfoList();
      String ret = null;
      for (UsbDeviceInfo info : list) {
        if (info.pathFile.getPath().equals(path)) {
          ret = info.label;
        }
      }
      if (ret == null) {
        ret = mNameMap.get(path);
        LogUtils.d(TAG, "get name from mNameMap, ret name=" + ret);
      }
      return ret;
    }

    private void sendEvent(String eventName, String productName, String filePath) {
      try {
        LogUtils.d(TAG, "sendEvent, eventName=" + eventName);
        HippyMap map = new HippyMap();
        map.pushString(EVENT_NAME, eventName);
        map.pushString(DEVICE_PRODUCT_NAME, productName);
        map.pushString(DEVICE_FILE_PATH, filePath);
        mContext.getModuleManager().getJavaScriptModule(EventDispatcher.class)
          .receiveNativeEvent(USB_DEVICE_EVENT_NAME, map);
      } catch (Throwable e) {
        LogUtils.d(TAG, "sendEvent ex= " + e.toString());
      }
    }
  }

}
