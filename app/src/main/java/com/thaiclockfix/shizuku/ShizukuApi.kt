package com.thaiclockfix.shizuku

import rikka.shizuku.Shizuku

internal interface ShizukuApi {
    fun addBinderReceivedListenerSticky(listener: Shizuku.OnBinderReceivedListener)

    fun removeBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener)

    fun addBinderDeadListener(listener: Shizuku.OnBinderDeadListener)

    fun removeBinderDeadListener(listener: Shizuku.OnBinderDeadListener)

    fun addRequestPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener)

    fun removeRequestPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener)

    fun pingBinder(): Boolean

    fun isPreV11(): Boolean

    fun checkSelfPermission(): Int

    fun requestPermission(requestCode: Int)
}

internal object AndroidShizukuApi : ShizukuApi {
    override fun addBinderReceivedListenerSticky(listener: Shizuku.OnBinderReceivedListener) {
        Shizuku.addBinderReceivedListenerSticky(listener)
    }

    override fun removeBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
        Shizuku.removeBinderReceivedListener(listener)
    }

    override fun addBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
        Shizuku.addBinderDeadListener(listener)
    }

    override fun removeBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
        Shizuku.removeBinderDeadListener(listener)
    }

    override fun addRequestPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
    }

    override fun removeRequestPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    override fun pingBinder(): Boolean = Shizuku.pingBinder()

    override fun isPreV11(): Boolean = Shizuku.isPreV11()

    override fun checkSelfPermission(): Int = Shizuku.checkSelfPermission()

    override fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }
}
