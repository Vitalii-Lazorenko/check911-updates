package com.example.check_911

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InstructionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        val title = intent.getStringExtra(EXTRA_INSTRUCTION_TITLE).orEmpty()
        val id = intent.getStringExtra(EXTRA_INSTRUCTION_ID).orEmpty()

        findViewById<TextView>(R.id.instructionsTitleTextView).text =
            if (title.isNotBlank()) "Інструкція: $title" else "Інструкція"

        findViewById<TextView>(R.id.instructionsMetaTextView).text =
            if (id.isNotBlank()) "ID: $id" else ""
    }

    companion object {
        const val EXTRA_INSTRUCTION_ID = "EXTRA_INSTRUCTION_ID"
        const val EXTRA_INSTRUCTION_TITLE = "EXTRA_INSTRUCTION_TITLE"
    }
}
