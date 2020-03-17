package com.fleeksoft.simple3diviapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.fleeksoft.simple3diviapp.ui.DiviBaseActivity
import com.fleeksoft.tmaattendancelocal.AppDatabase
import kotlinx.android.synthetic.main.activity_person_list.*
import kotlinx.coroutines.launch

class PersonListActivity : DiviBaseActivity() {
    val db: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person_list)
        setBackArrowEnabled(toolbar, "Registered Persons")
        loadPersons()
    }

    private fun loadPersons() {
        lifecycleScope.launch {
            val allPersons = db.employeeDao().getAll()
            if (allPersons.isEmpty()) {
                list_view_person.visibility = View.GONE
                text_empty_state.visibility = View.VISIBLE
            } else {
                list_view_person.visibility = View.VISIBLE
                text_empty_state.visibility = View.GONE
            }
            list_view_person.adapter = ArrayAdapter<String>(
                this@PersonListActivity,
                android.R.layout.simple_list_item_1,
                allPersons.map { it.personName })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.person_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_db) {
            lifecycleScope.launch {
                db.employeeDao().deleteAll()
                loadPersons()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
