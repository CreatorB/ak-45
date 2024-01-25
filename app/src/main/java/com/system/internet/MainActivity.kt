package com.system.internet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.min
import com.google.firebase.firestore.FieldValue

class MainActivity : AppCompatActivity() {

    private lateinit var dataListView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val LIMIT = 1000
    private var lastKey: String? = null
    private var isLoading = false

    var listItems = ArrayList<String>()
    var listKeys = ArrayList<String>()
    lateinit var adapter: ColorArrayAdapter
//    lateinit var adapter: ArrayAdapter<String>
    private val database = FirebaseDatabase.getInstance()
    private val dbRef = database.getReference("data")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        dataListView = findViewById(R.id.lv)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        adapter = ColorArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listItems
        )
//        adapter = ArrayAdapter<String>(
//            this,
//            android.R.layout.simple_list_item_1,
//            listItems
//        )
        dataListView.adapter = adapter

        dataListView.setOnItemLongClickListener { parent, view, position, id ->
            val itemToCopy = adapter.getItem(position)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("copied_data", itemToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            true // Return true to indicate the long click was handled
        }

        dataListView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}

            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                // Check if the last item is visible and not currently loading more data
                if (firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount != 0 && !isLoading) {
                    loadMoreData()
                }
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            // Refresh data when the user pulls down
            refreshData()
        }

        addChildEventListener()
    }

    private fun refreshData() {
        // Clear existing data
        listItems.clear()
        listKeys.clear()
        adapter.notifyDataSetChanged()

        // Reset lastKey
        lastKey = null

        // Load fresh data
//        loadMoreData()
        addChildEventListener()

        // Stop the refreshing animation
        swipeRefreshLayout.isRefreshing = false
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
            .setMessage("Are you sure you want to delete selected items?")
            .setPositiveButton("Yes") { dialog, which ->
                progressBar.visibility = View.VISIBLE
//                delete1000Data()
                deleteDataLoaded()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteDataLoaded() {
        listKeys.forEach { key ->
            dbRef.child(key).removeValue()
        }
        progressBar.visibility = View.GONE
        Toast.makeText(this, "Selected items deleted successfully", Toast.LENGTH_SHORT).show()
        refreshData()
    }

//    private fun delete1000Data() {
//        val batchSize = 1000 // Set your desired batch size
//        val totalItems = listKeys.size
//        var batchCount = 0
//
//        // Process data in batches
//        while (batchCount * batchSize < totalItems) {
//            val start = batchCount * batchSize
//            val end = min((batchCount + 1) * batchSize, totalItems)
//
//            val batchKeys = listKeys.subList(start, end).toList() // Create a copy of the sublist
//
//            if (batchKeys.isNotEmpty()) {
//                val batchUpdates = HashMap<String, Any?>()
//
//                for (key in batchKeys) {
//                    batchUpdates["$key"] = null
//                }
//
//                dbRef.updateChildren(batchUpdates)
//                    .addOnSuccessListener {
//                        // Remove processed keys from the local list
//                        listKeys.removeAll(batchKeys)
//
//                        // Update the adapter after each batch
//                        adapter.notifyDataSetChanged()
//
//                        // Hide the progress bar if all batches are processed
//                        if ((batchCount + 1) * batchSize >= totalItems) {
//                            progressBar.visibility = View.GONE
//                            Toast.makeText(
//                                this@MainActivity,
//                                "All data deleted successfully",
//                                Toast.LENGTH_SHORT
//                            ).show()
////                            for delete bilion data
////                            deleteLoadedData()
//                        }
//                    }
//                    .addOnFailureListener { e ->
//                        // Handle failure
//                        progressBar.visibility = View.GONE
//                        Toast.makeText(
//                            this@MainActivity,
//                            "Error deleting data: ${e.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        Log.e("cekDeleteAllData", "Error deleting data: ${e.message}")
//                    }
//            } else {
//                // No more keys to process
//                break
//            }
//
//            batchCount++
//        }
//    }

    private fun loadMoreData() {
        isLoading = true
        // Update the query to load more items based on the lastKey
        dbRef.orderByKey().startAt(lastKey).limitToFirst(LIMIT + 1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val dataCount = dataSnapshot.childrenCount.toInt()

                    if (dataCount > 0) {
                        val iterator = dataSnapshot.children.iterator()

                        // Skip the first item as it is the last key from the previous query
                        if (iterator.hasNext()) {
                            iterator.next()
                        }

                        while (iterator.hasNext()) {
                            val data = iterator.next()
                            lastKey = data.key

                            // Add the data to the list
                            listItems.add(data.child("log").value as String)
                            listKeys.add(data.key!!)
                        }

                        // Update the adapter
                        adapter.notifyDataSetChanged()
                    } else {
                        // No more data available
                        Toast.makeText(
                            this@MainActivity,
                            "No more data available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    isLoading = false
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    isLoading = false
                    // Handle onCancelled
                }
            })
    }

    private fun addChildEventListener() {
        val childListener: ChildEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                adapter.add(dataSnapshot.child("log").value as String?)
                listKeys.add(dataSnapshot.key!!)
                progressBar.visibility = View.GONE

                // Update the lastKey for "load more" functionality
                lastKey = dataSnapshot.key
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
        dbRef.orderByKey().limitToFirst(LIMIT).addChildEventListener(childListener)
    }
}