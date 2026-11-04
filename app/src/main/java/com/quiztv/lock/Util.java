package com.quiztv.lock;

import android.content.Context;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/** 局域网 IP 工具，用于在电视端和 WebUI 上显示访问地址 */
public class Util {
    public static String getIp(Context ctx) {
        return getIp();
    }

    public static String getIp() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(ifs)) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (Object o : Collections.list(ni.getInetAddresses())) {
                    InetAddress a = (InetAddress) o;
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return "未知";
    }
}
