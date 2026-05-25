package com.example.check_911

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.check_911.data.db.entity.UsersEntity
import com.example.check_911.data.utils.AppLogger
import org.koin.androidx.viewmodel.ext.android.viewModel


class SelectionActivity : AppCompatActivity() {
    private lateinit var userSearchView: AutoCompleteTextView
    private lateinit var nextButton: Button
    private val selectionViewModel: SelectionViewModel by viewModel()
    private var selectedUser: UsersEntity? = null

//    03.04.26
//    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var adapter: UserAdapter
    private var userList: List<UsersEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)

        userSearchView = findViewById(R.id.userSearchView)
        nextButton = findViewById(R.id.nextButton)

        setupUserSearch()

        nextButton.setOnClickListener {
            if (selectedUser != null) {
                selectionViewModel.saveSelectedUser(selectedUser!!, this)
                startActivity(Intent(this, StoreActivity::class.java))
            } else {
                Toast.makeText(this, "Будь ласка, виберіть користувача", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    03.04.26
//    private fun setupUserSearch() {
//        selectionViewModel.users.observe(this) { users ->
//            if (users.isNotEmpty()) {
//                userList = users
//                val names = users.map { it.userName }
//
//                adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
//                userSearchView.setAdapter(adapter)
//
//                // Порог ввода — 0 символов (чтобы сразу показывался список)
//                userSearchView.threshold = 0
//
//                // При клике открываем список
//                userSearchView.setOnClickListener {
//                    if (!userSearchView.isPopupShowing) {
//                        userSearchView.showDropDown()
//                    }
//                }
//
//                // При выборе имени находим пользователя
//                userSearchView.setOnItemClickListener { _, _, position, _ ->
//                    val name = adapter.getItem(position)
//                    selectedUser = userList.firstOrNull { it.userName == name }
//                }
//            } else {
//                Toast.makeText(this, "Користувачі не знайдені", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private fun setupUserSearch() {
        selectionViewModel.users.observe(this) { users ->
            if (users.isNotEmpty()) {
                userList = users

                adapter = UserAdapter(this, users)
                userSearchView.setAdapter(adapter)

                userSearchView.threshold = 0

                userSearchView.setOnClickListener {
                    if (!userSearchView.isPopupShowing) {
                        userSearchView.showDropDown()
                    }
                }

                userSearchView.setOnItemClickListener { _, _, position, _ ->
                    selectedUser = adapter.getItem(position)
                }

            } else {
                Toast.makeText(this, "Користувачі не знайдені", Toast.LENGTH_SHORT).show()
            }
        }
    }

}



//class SelectionActivity : AppCompatActivity() {
//
//    private lateinit var storeSpinner: Spinner
//    private lateinit var userSpinner: Spinner
//    private lateinit var nextButton: Button
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_selection)
//
////        storeSpinner = findViewById(R.id.storeSpinner)
//        userSpinner = findViewById(R.id.userSpinner)
//        nextButton = findViewById(R.id.nextButton)
//
//        // Настройка адаптеров для спиннеров
//        setupSpinners()
//
//        nextButton.setOnClickListener {
//            // Переход на следующую активити
//            val intent = Intent(this, StoreActivity::class.java) // Переход на следующую активити
//            startActivity(intent)
//        }
//    }
//
//    private fun setupSpinners() {
//        // Пример данных для спиннеров
////        val stores = arrayOf("Точка 1", "Точка 2", "Точка 3")
//        val users = arrayOf("Користувач 1", "Користувач 2", "Користувач 3")
//
////        val storeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stores)
////        storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
////        storeSpinner.adapter = storeAdapter
//
//        val userAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, users)
//        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        userSpinner.adapter = userAdapter
//    }
//}