package cn.reactnative.modules.qq;


import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import com.tencent.connect.UserInfo; 
import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONObject;

import java.util.Date;

/**
 * Created by tdzl2_000 on 2015-10-10.
 *
 * Modified by Renguang Dong on 2016-05-25.
 */
public class QQModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private String appId;
    private Tencent api;
    private IUiListener loginListener; //授权登录监听器  
    private IUiListener userInfoListener; //获取用户信息监听器 
    private IUiListener shareListener; // 分享监听器
    private UserInfo userInfo; //qq用户信息
    private final static String INVOKE_FAILED = "QQ API invoke returns false.";
    private boolean isLogin;

    private static final String RCTQQShareTypeNews = "news";
    private static final String RCTQQShareTypeImage = "image";
    private static final String RCTQQShareTypeText = "text";
    private static final String RCTQQShareTypeVideo = "video";
    private static final String RCTQQShareTypeAudio = "audio";

    private static final String RCTQQShareType = "type";
    private static final String RCTQQShareText = "text";
    private static final String RCTQQShareTitle = "title";
    private static final String RCTQQShareDescription = "description";
    private static final String RCTQQShareWebpageUrl = "webpageUrl";
    private static final String RCTQQShareImageUrl = "imageUrl";

    private static final int SHARE_RESULT_CODE_SUCCESSFUL = 0;
    private static final int SHARE_RESULT_CODE_FAILED = 1;
    private static final int SHARE_RESULT_CODE_CANCEL = 2;

    public QQModule(ReactApplicationContext context) {
        super(context);
        ApplicationInfo appInfo = null;
        try {
            appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Error(e);
        }
        if (!appInfo.metaData.containsKey("QQ_APPID")){
            throw new Error("meta-data QQ_APPID not found in AndroidManifest.xml");
        }
        this.appId = appInfo.metaData.get("QQ_APPID").toString();
        InitData();
    }

    public void InitData() {
        loginListener = new IUiListener() {  
            @Override  
            public void onError(UiError arg0) {  
                // TODO Auto-generated method stub  
                WritableMap resultMap = Arguments.createMap();
                resultMap.putInt("errCode", SHARE_RESULT_CODE_FAILED);
                resultMap.putString("message", arg0.errorMessage);

                getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp", resultMap);

                Toast.makeText(getCurrentActivity(), "登录失败："+ arg0.errorMessage, Toast.LENGTH_LONG).show();
            }  
            @Override  
            public void onComplete(Object value) {  
                // TODO Auto-generated method stub  
                if (value == null) {  
                    return;  
                }
                WritableMap resultMap = Arguments.createMap();
                resultMap.putString("type", "QQAuthorizeResponse");
                try {
                    JSONObject obj = (JSONObject) (value);
                        
                    String openID = obj.getString(Constants.PARAM_OPEN_ID);  
                    String accessToken = obj.getString(Constants.PARAM_ACCESS_TOKEN);
                    String expires = obj.getString(Constants.PARAM_EXPIRES_IN);
                    api.setOpenId(openID);
                    api.setAccessToken(accessToken, expires);
                    
                    resultMap.putInt("errCode", 0);
                    resultMap.putString("openid", openID);
                    resultMap.putString("access_token", accessToken);
                    resultMap.putString("oauth_consumer_key", appId);
                    resultMap.putDouble("expires_in", (new Date().getTime() + obj.getLong(Constants.PARAM_EXPIRES_IN)));
                    getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp",resultMap);

                    Toast.makeText(getCurrentActivity(), "登录成功", Toast.LENGTH_LONG).show();
                } catch (Exception e){
                    WritableMap map = Arguments.createMap();
                    map.putInt("errCode", Constants.ERROR_UNKNOWN);
                    map.putString("errMsg", e.getLocalizedMessage());

                    getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp",map);
                    Toast.makeText(getCurrentActivity(), "登录失败：未知错误", Toast.LENGTH_LONG).show();
                    return;
                }
            }  
  
            @Override  
            public void onCancel() {  
                // TODO Auto-generated method stub  
                WritableMap resultMap = Arguments.createMap();
                resultMap.putInt("errCode", SHARE_RESULT_CODE_CANCEL);
                resultMap.putString("message", "登录取消");
                getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp",resultMap);

                Toast.makeText(getCurrentActivity(), "登录取消", Toast.LENGTH_LONG).show();
            }  
        };  
          
        userInfoListener = new IUiListener() {  
            @Override  
            public void onError(UiError arg0) {  
                // TODO Auto-generated method stub  
                WritableMap resultMap = Arguments.createMap();
                resultMap.putInt("errCode", SHARE_RESULT_CODE_FAILED);
                resultMap.putString("message", arg0.errorMessage);

                getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp",resultMap);

                Toast.makeText(getCurrentActivity(), "获取用户失败："+ arg0.errorMessage, Toast.LENGTH_LONG).show();
            }  
              
            @Override  
            public void onComplete(Object arg0) {  
                // TODO Auto-generated method stub  
                if(arg0 == null){  
                    return;  
                }  
                WritableMap resultMap = Arguments.createMap();
                resultMap.putString("type", "QQUserInfoResponse");
                try {  
                    JSONObject jo = (JSONObject) arg0;  
                    String nickName = jo.getString("nickname");  
                    String gender = jo.getString("gender");
                    String avatar = jo.getString("figureurl_qq_2");
                    String openId = api.getOpenId();
  
                    resultMap.putInt("errCode", 0);
                    resultMap.putString("nickname", nickName);
                    resultMap.putString("gender", gender);
                    resultMap.putString("avatar", avatar);
                    resultMap.putString("openid", openId);

                    getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp",resultMap);

                    Toast.makeText(getCurrentActivity(), "登录成功，您好，" + nickName, Toast.LENGTH_LONG).show();
  
                } catch (Exception e) {  
                    WritableMap map = Arguments.createMap();
                    map.putInt("errCode", Constants.ERROR_UNKNOWN);
                    map.putString("errMsg", e.getLocalizedMessage());

                    getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp", map);
                    Toast.makeText(getCurrentActivity(), "获取用户失败：未知错误", Toast.LENGTH_LONG).show();
                    return;
                }  
            }  
              
            @Override  
            public void onCancel() {  
                // TODO Auto-generated method stub  
                WritableMap resultMap = Arguments.createMap();
                resultMap.putInt("errCode", SHARE_RESULT_CODE_CANCEL);
                resultMap.putString("message", "获取用户信息取消");
                getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp", resultMap);

                Toast.makeText(getCurrentActivity(), "获取用户信息取消", Toast.LENGTH_LONG).show();
            }  
        };  

        shareListener = new IUiListener() {  
              
            @Override  
            public void onError(UiError arg0) {  
                // TODO Auto-generated method stub  
                WritableMap resultMap = Arguments.createMap();
                resultMap.putInt("errCode", SHARE_RESULT_CODE_FAILED);
                resultMap.putString("message", arg0.errorMessage);

                getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp", resultMap);

                Toast.makeText(getCurrentActivity(), "分享失败："+ arg0.errorMessage, Toast.LENGTH_LONG).show();
            }  
              
            @Override  
            public void onComplete(Object arg0) {  
                // TODO Auto-generated method stub  
                if(arg0 == null){  
                    return;  
                }  
                WritableMap resultMap = Arguments.createMap();
                resultMap.putString("type", "QQShareResponse");
                resultMap.putInt("errCode", SHARE_RESULT_CODE_SUCCESSFUL);
                resultMap.putString("message", "Share successfully.");
                getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp", resultMap);

                Toast.makeText(getCurrentActivity(), "分享成功", Toast.LENGTH_LONG).show();
            }  
              
            @Override  
            public void onCancel() {  
                // TODO Auto-generated method stub  
                WritableMap resultMap = Arguments.createMap();
                resultMap.putInt("errCode", SHARE_RESULT_CODE_CANCEL);
                resultMap.putString("message", "分享取消");
                getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp", resultMap);

                Toast.makeText(getCurrentActivity(), "分享取消", Toast.LENGTH_LONG).show();
            }  
        };  
    }


    @Override
    public void initialize() {
        super.initialize();

        if (api == null) {
            api = Tencent.createInstance(appId, getReactApplicationContext().getApplicationContext());
        }
        getReactApplicationContext().addActivityEventListener(this);
    }

    @Override
    public void onCatalystInstanceDestroy() {

        if (api != null){
            api = null;
        }
        getReactApplicationContext().removeActivityEventListener(this);

        super.onCatalystInstanceDestroy();
    }

    @Override
    public String getName() {
        return "RCTQQAPI";
    }

    @ReactMethod
    public void isQQInstalled(Promise promise) {
        if (api.isSupportSSOLogin(getCurrentActivity())) {
            promise.resolve(true);
        }
        else {
            promise.reject("not installed");
        }
    }

    @ReactMethod
    public void isQQSupportApi(Promise promise) {
        if (api.isSupportSSOLogin(getCurrentActivity())) {
            promise.resolve(true);
        }
        else {
            promise.reject("not support");
        }
    }

    @ReactMethod
    public void login(String scopes, Promise promise){
        this.isLogin = true;
        if (!api.isSessionValid()){
            api.login(getCurrentActivity(), scopes == null ? "get_simple_userinfo" : scopes, loginListener);
            promise.resolve(null);
        }else {
            promise.resolve(null);

            WritableMap resultMap = Arguments.createMap();
            resultMap.putInt("errCode", 0);
            resultMap.putString("openid", api.getOpenId());
            resultMap.putString("access_token", api.getAccessToken());
            resultMap.putDouble("expires_in", (new Date().getTime() + api.getExpiresIn()));

            getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit("QQ_Resp",resultMap);
        }
    }
    
    @ReactMethod
    public void getUserInfo(Promise promise){
        if(this.isLogin){
            userInfo = new UserInfo(getReactApplicationContext(), api.getQQToken());
            userInfo.getUserInfo(userInfoListener);
            promise.resolve(null);
        }else {
            promise.reject(INVOKE_FAILED);
        }
    }

    @ReactMethod
    public void shareToQQ(ReadableMap data, Promise promise){
        this._shareToQQ(data, 0);
        promise.resolve(null);
    }

    @ReactMethod
    public void shareToQzone(ReadableMap data, Promise promise){
        this._shareToQQ(data, 1);
        promise.resolve(null);
    }

    private void _shareToQQ(ReadableMap data, int scene) {
        this.isLogin = false;
        Bundle bundle = new Bundle();
        if (data.hasKey(RCTQQShareTitle)){
            bundle.putString(QQShare.SHARE_TO_QQ_TITLE, data.getString(RCTQQShareTitle));
        }
        if (data.hasKey(RCTQQShareDescription)){
            bundle.putString(QQShare.SHARE_TO_QQ_SUMMARY, data.getString(RCTQQShareDescription));
        }
        if (data.hasKey(RCTQQShareWebpageUrl)){
            bundle.putString(QQShare.SHARE_TO_QQ_TARGET_URL, data.getString(RCTQQShareWebpageUrl));
        }
        if (data.hasKey(RCTQQShareImageUrl)){
            bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, data.getString(RCTQQShareImageUrl));
        }
        if (data.hasKey("appName")){
            bundle.putString(QQShare.SHARE_TO_QQ_APP_NAME, data.getString("appName"));
        }

        String type = RCTQQShareTypeNews;
        if (data.hasKey(RCTQQShareType)) {
            type = data.getString(RCTQQShareType);
        }

        if (type.equals(RCTQQShareTypeNews)){
            bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        } else if (type.equals(RCTQQShareTypeImage)){
            bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
            bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, data.getString(RCTQQShareImageUrl));
        } else if (type.equals(RCTQQShareTypeAudio)) {
            bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_AUDIO);
            if (data.hasKey("flashUrl")){
                bundle.putString(QQShare.SHARE_TO_QQ_AUDIO_URL, data.getString("flashUrl"));
            }
        } else if (type.equals("app")){
            bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_APP);
        }

        Log.e("QQShare", bundle.toString());

        if (scene == 0 ) {
            // Share to QQ.
            bundle.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE);
            api.shareToQQ(getCurrentActivity(), bundle, shareListener);
        }
        else if (scene == 1) {
            // Share to Qzone.
            bundle.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN);
            api.shareToQQ(getCurrentActivity(), bundle, shareListener);
        }
    }

    private String _getType() {
        return (this.isLogin?"QQAuthorizeResponse":"QQShareResponse");
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        Tencent.onActivityResultData(requestCode, resultCode, data, loginListener);
    }

    public void onNewIntent(Intent intent){

    }
}
