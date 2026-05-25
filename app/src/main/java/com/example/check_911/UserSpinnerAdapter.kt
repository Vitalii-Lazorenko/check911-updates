package com.example.check_911

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.check_911.data.db.entity.UsersEntity

class UserSpinnerAdapter(
    context: Context,
    private val users: List<UsersEntity>
) : ArrayAdapter<UsersEntity>(context, android.R.layout.simple_spinner_item, users) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)

        if (position == 0) {
            textView.text = "— Виберіть користувача —"
        } else {
            val user = users[position]
            textView.text = "${user.userName} (${user.positionName})"
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)

        if (position == 0) {
            textView.text = "— Виберіть користувача —"
        } else {
            val user = users[position]
            textView.text = "${user.userName} (${user.positionName})"
        }

        return view
    }

    override fun getCount(): Int {
        return users.size
    }
}
