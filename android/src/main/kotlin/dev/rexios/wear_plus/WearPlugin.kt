package dev.rexios.wear_plus

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.lifecycle.*
import com.google.android.wearable.compat.WearableActivityController
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class WearPlugin : FlutterPlugin, ActivityAware, MethodChannel.MethodCallHandler, LifecycleEventObserver {
    private var ambientCallback = WearableAmbientCallback()
    private var methodChannel: MethodChannel? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var ambientController: WearableActivityController? = null

    companion object {
        private const val CHANNEL_NAME = "wear"
        private const val BURN_IN_PROTECTION = WearableActivityController.EXTRA_BURN_IN_PROTECTION
        private const val LOW_BIT_AMBIENT = WearableActivityController.EXTRA_LOWBIT_AMBIENT
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        methodChannel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        attachAmbientController(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        detachAmbientController()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        attachAmbientController(binding)
    }

    override fun onDetachedFromActivity() {
        detachAmbientController()
    }

    private fun attachAmbientController(binding: ActivityPluginBinding) {
        activityBinding = binding
        ambientController = WearableActivityController(CHANNEL_NAME, binding.activity, ambientCallback)
        ambientController?.setAmbientEnabled()
        (binding.lifecycle as? HiddenLifecycleReference)?.lifecycle?.addObserver(this)
    }

    private fun detachAmbientController() {
        activityBinding?.let {
            (it.lifecycle as? HiddenLifecycleReference)?.lifecycle?.removeObserver(this)
        }
        activityBinding = null
        ambientController = null
    }

    private fun isWearOS(activity: Activity): Boolean {
        val packageManager = activity.packageManager
        val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

        return packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) ||
                uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_WATCH
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val activity = activityBinding?.activity
        if (activity == null) {
            result.error("no-activity", "No android activity available.", null)
            return
        }

        when (call.method) {
            "getShape" -> {
                val shape = if (activity.resources.configuration.isScreenRound) "round" else "square"
                result.success(shape)
            }

            "isWearOs" -> {
                result.success(isWearOS(activity))
            }

            "isAmbient" -> {
                result.success(ambientController?.isAmbient ?: false)
            }

            "setAutoResumeEnabled" -> {
                val enabled = call.argument<Boolean>("enabled") ?: false
                ambientController?.setAutoResumeEnabled(enabled)
                result.success(null)
            }

            "setAmbientOffloadEnabled" -> {
                val enabled = call.argument<Boolean>("enabled") ?: false
                ambientController?.setAmbientOffloadEnabled(enabled)
                result.success(null)
            }

            else -> result.notImplemented()
        }
    }

    inner class WearableAmbientCallback : WearableActivityController.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle) {
            val burnInProtection = ambientDetails.getBoolean(BURN_IN_PROTECTION, false)
            val lowBitAmbient = ambientDetails.getBoolean(LOW_BIT_AMBIENT, false)
            methodChannel?.invokeMethod("onEnterAmbient", mapOf(
                "burnInProtection" to burnInProtection,
                "lowBitAmbient" to lowBitAmbient
            ))
        }

        override fun onExitAmbient() {
            methodChannel?.invokeMethod("onExitAmbient", null)
        }

        override fun onUpdateAmbient() {
            methodChannel?.invokeMethod("onUpdateAmbient", null)
        }

        override fun onInvalidateAmbientOffload() {
            methodChannel?.invokeMethod("onInvalidateAmbientOffload", null)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> ambientController?.onCreate()
            Lifecycle.Event.ON_RESUME -> ambientController?.onResume()
            Lifecycle.Event.ON_PAUSE -> ambientController?.onPause()
            Lifecycle.Event.ON_STOP -> ambientController?.onStop()
            Lifecycle.Event.ON_DESTROY -> ambientController?.onDestroy()
            else -> {}
        }
    }
}
