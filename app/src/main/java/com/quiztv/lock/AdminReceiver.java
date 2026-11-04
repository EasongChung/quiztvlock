package com.quiztv.lock;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * 设备管理员 / 设备所有者组件：实现「防卸载」。
 * <ul>
 *   <li>普通设备管理员：激活后系统设置中无法直接卸载本应用（需先撤销管理员）；</li>
 *   <li>设备所有者（ADB 执行 dpm set-device-owner 一次）：可直接禁用卸载按钮，
 *       且免于国产 TV 系统的自启动白名单限制，开机自启更可靠。</li>
 * </ul>
 * 家长卸载路径：WebUI 登录 → 防卸载卡片输入 PIN → 解除保护。
 */
public class AdminReceiver extends DeviceAdminReceiver {

    public static ComponentName componentName(Context ctx) {
        return new ComponentName(ctx, AdminReceiver.class);
    }

    public static DevicePolicyManager dpm(Context ctx) {
        return (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public static boolean isAdminActive(Context ctx) {
        try {
            return dpm(ctx).isAdminActive(componentName(ctx));
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isDeviceOwner(Context ctx) {
        try {
            return dpm(ctx).isDeviceOwnerApp(ctx.getPackageName());
        } catch (Throwable t) {
            return false;
        }
    }

    /** 是否已阻止卸载 */
    public static boolean isUninstallBlocked(Context ctx) {
        if (isDeviceOwner(ctx)) {
            try {
                return dpm(ctx).isUninstallBlocked(componentName(ctx), ctx.getPackageName());
            } catch (Throwable t) {
                return false;
            }
        }
        return isAdminActive(ctx);
    }

    /** 设置/解除卸载拦截，需设备所有者身份；成功返回 true */
    public static boolean setUninstallBlocked(Context ctx, boolean block) {
        if (!isDeviceOwner(ctx)) return false;
        dpm(ctx).setUninstallBlocked(componentName(ctx), ctx.getPackageName(), block);
        return true;
    }

    /** 请求成为设备管理员（弹出系统授权页） */
    public static void requestAdmin(android.app.Activity act, int requestCode) {
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(act));
        act.startActivityForResult(i, requestCode);
    }

    /** 孩子想撤销管理员时，系统弹出的警告文案 */
    @Override
    public CharSequence onDisableRequested(Context ctx, Intent intent) {
        return "移除「答题电视锁」的设备管理员后将允许卸载本应用，请家长确认后操作。";
    }
}
