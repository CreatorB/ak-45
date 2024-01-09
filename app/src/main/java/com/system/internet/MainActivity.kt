package com.system.internet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase


class MainActivity : AppCompatActivity() {

    private  lateinit var dataListView: ListView

    var listItems = ArrayList<String>()
    var listKeys = ArrayList<String>()
    lateinit var adapter: ArrayAdapter<String>
    private val database = FirebaseDatabase.getInstance()
    private val dbRef = database.getReference("data")
    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        dataListView = findViewById(R.id.lv)

        adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            listItems
        )
        dataListView.setAdapter(adapter)
//        dataListView.choiceMode = ListView.CHOICE_MODE_SINGLE

        dataListView.setOnItemLongClickListener { parent, view, position, id ->
            val itemToCopy = adapter.getItem(position)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("copied_data", itemToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            true // Return true to indicate the long click was handled
        }
        addChildEventListener();
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                deleteData()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteData() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete all data?")
            .setPositiveButton("Yes") { dialog, which ->
                progressBar.visibility = View.VISIBLE
                // If the user confirms, delete the data
                dbRef.removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Data deleted successfully", Toast.LENGTH_SHORT).show()
                        // Clear local lists
                        listItems.clear()
                        listKeys.clear()
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this, "Failed to delete data", Toast.LENGTH_SHORT).show()
                    }
                    progressBar.visibility = View.GONE
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun addChildEventListener() {
        val childListener: ChildEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                adapter.add(
                    dataSnapshot.child("log").value as String?
                )
                listKeys.add(dataSnapshot.key!!)
                progressBar.visibility = View.GONE
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val key = dataSnapshot.key
                val index = listKeys.indexOf(key)
                if (index != -1) {
                    listItems.removeAt(index)
                    listKeys.removeAt(index)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        dbRef.addChildEventListener(childListener)
    }
}
