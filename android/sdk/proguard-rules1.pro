# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Users\xiandongluo\AppData\Local\Android\sdk1/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#-optimizationpasses 7
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#noinspection ShrinkerUnresolvedReference
#-overloadaggressively



#<!-------------------  下方是android平台自带的排除项，这里不要动         ---------------->


# <!--改成这样,因为发现部分SDK中没有被java代码显示调用过,SO文件中调用的native方法会被混淆掉-->


#<!--------------------  下方是共性的排除项目         ------------------>
# <!--方法名中含有“JNI”字符的，认定是Java Native Interface方法，自动排除-->
# <!--方法名中含有“JRI”字符的，认定是Java Reflection Interface方法，自动排除-->

-keepattributes Exceptions

-keep class com.tencent.mtt.hippy.*{*;}


-keep class com.tencent.mtt.hippy.common.* {*;}

-keep class com.tencent.mtt.hippy.annotation.* {*;}

-keep class com.tencent.mtt.hippy.adapter.** {*;}

-keep class com.tencent.mtt.hippy.modules.* {*;}


-keep class * extends com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase{   public *;}

-keep class com.tencent.mtt.hippy.modules.nativemodules.* {*;}

-keep class com.tencent.mtt.hippy.modules.javascriptmodules.* {*;}

-keep class com.tencent.mtt.hippy.bridge.bundleloader.* {*;}

-keep class com.tencent.mtt.hippy.uimanager.* {*;}
-keep class com.tencent.mtt.hippy.uimanager.DiffUtils {*;}
-keep class com.tencent.mtt.hippy.uimanager.DiffUtils$* {*;}
-keep class com.tencent.mtt.hippy.uimanager.HippyCustomViewCreator {*;}
-keep class com.tencent.mtt.hippy.uimanager.HippyViewController {*;}
-keep class com.tencent.mtt.hippy.uimanager.HippyViewBase {*;}
-keep class com.tencent.mtt.hippy.uimanager.HippyGroupController {*;}
-keep class com.tencent.mtt.hippy.uimanager.HippyViewEvent {*;}
-keep class com.tencent.mtt.hippy.uimanager.RenderNode {*;}
-keep class com.tencent.mtt.hippy.uimanager.RenderManager {*;}
#-keepclasseswithmembers class com.tencent.mtt.hippy.uimanager.RenderManager {
#public com.tencent.mtt.hippy.uimanager.RenderNode getRenderNode(int);
#public com.tencent.mtt.hippy.uimanager.ControllerManager getControllerManager();
#public void addUpdateNodeIfNeeded(com.tencent.mtt.hippy.uimanager.RenderNode);
#}
-keep class com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher {*;}
-keep class com.tencent.mtt.hippy.uimanager.ListItemRenderNode{
public boolean shouldSticky();
}
-keepclasseswithmembers class com.tencent.mtt.hippy.uimanager.ControllerManager {
public android.view.View findView(int);
public void deleteChild(int ,int );
}
#-keepclasseswithmembers class com.tencent.mtt.hippy.dom.DomManager {
#public void updateNodeSize(int,int,int);
#public void forceUpdateNode(int);
#public void addActionInterceptor(com.tencent.mtt.hippy.dom.node.DomActionInterceptor);
#public void removeActionInterceptor(com.tencent.mtt.hippy.dom.node.DomActionInterceptor);
#
#}
-keepclasseswithmembers class com.tencent.mtt.hippy.bridge.HippyBridgeImpl {
*;
}

-keepclasseswithmembers class com.tencent.mtt.hippy.bridge.NativeCallback {
*;
}

-keep public interface com.tencent.mtt.hippy.modules.nativemodules.deviceevent.DeviceEventModule$InvokeDefaultBackPress {*;}

-keep class com.tencent.mtt.hippy.utils.* {*;}
#-keep class com.tencent.mtt.hippy.dom.node.NodeProps {*;}
#-keep class com.tencent.mtt.hippy.dom.node.TextExtra {*;}
#-keep class * extends com.tencent.mtt.hippy.dom.node.DomNode {*;}

-keep class com.tencent.mtt.hippy.views.** {*;}
-keep interface com.tencent.mtt.hippy.bridge.HippyBridge {*;}

-keep class com.tencent.mtt.supportui.views.** {*;}
-keep class com.tencent.mtt.hippy.utils.** {*;}
#-keep class com.tencent.mtt.hippy.dom.node.TypeFaceUtil {*;}
-keep class com.tencent.mtt.hippy.adapter.image.HippyImageLoader {*;}
#-keep class com.tencent.mtt.hippy.dom.node.DomActionInterceptor {*;}
#-keep class com.tencent.mtt.hippy.dom.node.DomNodeRecord {*;}

-keepclasseswithmembers class com.tencent.mtt.supportui.** {
public <methods>;
}

-keepclasseswithmembers class com.tencent.mtt.supportui.** {
protected <methods>;
}

-keepclasseswithmembers class com.tencent.mtt.supportui.** {
public <fields>;
}

-keepclasseswithmembers class com.tencent.mtt.supportui.** {
protected <fields>;
}

-keepclasseswithmembers class android.support.v7.widget.** {
public <methods>;
}

-keepclasseswithmembers class android.support.v7.widget.** {
protected <methods>;
}

-keepclasseswithmembers class android.support.v7.widget.** {
public <fields>;
}

-keepclasseswithmembers class android.support.v7.widget.** {
protected <fields>;
}

-keep class com.quicktvui.hippyext.IEsComponentTag{*;}
# turbo
-keep class com.tencent.mtt.hippy.annotation.HippyTurboObj

-keep @com.tencent.mtt.hippy.annotation.HippyTurboObj class * {*;}

-keep class com.tencent.mtt.hippy.annotation.HippyTurboProp

-keepclasseswithmembers class * {
    @com.tencent.mtt.hippy.annotation.HippyTurboProp <methods>;
}

-keep class * extends com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase{
    public <methods>;
}

-keep class * extends com.tencent.mtt.hippy.uimanage.HippyViewController{
    public <methods>;
}

-keep public class com.tencent.mtt.hippy.bridge.jsi.TurboModuleManager {
public <fields>;
public <methods>;
}
-keep class * extends com.tencent.mtt.hippy.dom.node.StyleNode{*;}

#-keep class com.tencent.mtt.hippy.dom.flex.FlexMeasureMode {*;}
-keep class com.tencent.mtt.hippy.dom.** {*;}
-keep class com.quicktvui.hippyext.** {*;}
-keep class com.tencent.**{*;}
#自定义选集样式
-keep class com.tencent.smtt.flexbox.** {*;}
