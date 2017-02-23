/**
 * Created by Yun on 2015-12-12.
 */
import {NativeModules, NativeAppEventEmitter} from 'react-native';

const {QQAPI} = NativeModules;

function translateError(err, result) {
    if (!err) {
        return this.resolve(result);
    }
    if (typeof err === 'object') {
        if (err instanceof Error) {
            return this.reject(ret);
        }
        return this.reject(Object.assign(new Error(err.message), { errCode: err.errCode }));
    } else if (typeof err === 'string') {
        return this.reject(new Error(err));
    }
    this.reject(Object.assign(new Error(), { origin: err }));
}

export const isQQInstalled = QQAPI.isQQInstalled;
export const isQQSupportApi = QQAPI.isQQSupportApi;

// Save callback and wait for future event.
let savedCallback = undefined;
function waitForResponse(type) {
    return new Promise((resolve, reject) => {
        if (savedCallback) {
            savedCallback('User canceled.');
        }
        savedCallback = result => {
            savedCallback = undefined;
            if (result.errCode !== 0) {
                reject(result);
            } else {
                const {type, ...r} = result
                resolve(r);
            }
        };
    });
}

NativeAppEventEmitter.addListener('QQ_Resp', resp => {
    const callback = savedCallback;
    savedCallback = undefined;
    callback && callback(resp);
});

export function login(scopes) {
    return QQAPI.login(scopes)
        .then(() => waitForResponse("QQAuthorizeResponse"));
}

export function getUserInfo() {
    return QQAPI.getUserInfo()
        .then(() => waitForResponse("QQUserInfoResponse"));
}

export function shareToQQ(data={}) {
    return QQAPI.shareToQQ(data)
        .then(() => waitForResponse("QQShareResponse"));
}

export function shareToQzone(data={}) {
    return QQAPI.shareToQzone(data)
        .then(() => waitForResponse("QQShareResponse"));
}

export function logout(){
    QQAPI.logout()
}




