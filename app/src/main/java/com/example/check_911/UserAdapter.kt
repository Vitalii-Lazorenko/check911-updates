package com.example.check_911

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.example.check_911.data.db.entity.UsersEntity

class UserAdapter(
    context: Context,
    private val users: List<UsersEntity>
) : ArrayAdapter<UsersEntity>(context, android.R.layout.simple_dropdown_item_1line, users) {

    private var filteredUsers: List<UsersEntity> = users

    override fun getCount(): Int = filteredUsers.size

    override fun getItem(position: Int): UsersEntity = filteredUsers[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val user = filteredUsers[position]

        view.text = "${user.userName} (${user.positionName ?: ""})"

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase()?.trim() ?: ""

                val resultList = if (query.isEmpty()) {
                    users
                } else {
                    users.filter {
                        it.userName.lowercase().contains(query) ||
                                (it.positionName?.lowercase()?.contains(query) == true)
                    }
                }

                return FilterResults().apply {
                    values = resultList
                    count = resultList.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredUsers = results?.values as? List<UsersEntity> ?: emptyList()
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                val user = resultValue as UsersEntity
                return "${user.userName} (${user.positionName ?: ""})"
            }
        }
    }
}