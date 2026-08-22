package com.amazic.library.Utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.amazic.library.ads.splash_ads.AdmobAdsConfig
import com.amazic.library.ads.splash_ads.AsyncSplash
import java.net.NetworkInterface

class DetectionUtil {
    @SuppressLint("ServiceCast")
    fun isVpnActive(context: Context): Boolean {
        if (AdmobAdsConfig.getInstance().isUseDetectionVPNOrEmulator) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.allNetworks.any { network ->
                    cm.getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                }
            } else {
                try {
                    NetworkInterface.getNetworkInterfaces()?.toList()?.any { netInterface ->
                        netInterface.isUp && (
                                netInterface.name.equals("tun0", ignoreCase = true) ||
                                        netInterface.name.equals("ppp0", ignoreCase = true) ||
                                        netInterface.name.startsWith("tun", ignoreCase = true)
                                )
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            return false
        }
    }

    fun isEmulator(): Boolean {
        if (AdmobAdsConfig.getInstance().isUseDetectionVPNOrEmulator) {
            val isEmulatorBuild =
                Build.FINGERPRINT.startsWith("generic") ||
                        Build.FINGERPRINT.startsWith("unknown") ||
                        Build.FINGERPRINT.contains("emulator") ||
                        Build.FINGERPRINT.contains("sdk_gphone") ||
                        Build.MODEL.contains("google_sdk") ||
                        Build.MODEL.contains("Emulator") ||
                        Build.MODEL.contains("Android SDK built for x86") ||
                        Build.MODEL.contains("sdk_gphone") ||
                        Build.MANUFACTURER.contains("Genymotion") ||
                        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                        Build.PRODUCT == "google_sdk" ||
                        Build.PRODUCT.contains("sdk_gphone") ||
                        Build.PRODUCT.contains("emulator") ||
                        Build.HARDWARE.contains("goldfish") ||
                        Build.HARDWARE.contains("ranchu")

            // Kiểm tra file đặc trưng của emulator
            val isEmulatorFiles = listOf(
                "/dev/socket/qemud",
                "/dev/qemu_pipe",
                "/system/lib/libc_malloc_debug_qemu.so",
                "/sys/qemu_trace",
                "/system/bin/qemu-props"
            ).any { java.io.File(it).exists() }

            return isEmulatorBuild || isEmulatorFiles
        } else {
            return false
        }
    }
}