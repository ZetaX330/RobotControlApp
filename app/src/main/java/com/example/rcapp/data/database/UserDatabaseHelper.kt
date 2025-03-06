package com.example.rcapp.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.rcapp.model.UserInstruction

class UserDatabaseHelper (context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // trimIndent 用于去除多行字符串中每行的公共前导空格
        val createUsersTable = """
            CREATE TABLE user (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                phone TEXT
            )
        """.trimIndent()

        val createUserInstructionTable = """
            CREATE TABLE user_instruction (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                phone TEXT,
                instruction_name TEXT,
                media_path TEXT,
                instruction_path TEXT,
               
                FOREIGN KEY(phone) REFERENCES user(phone)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createUserInstructionTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS user_instruction")
        db.execSQL("DROP TABLE IF EXISTS user")
        onCreate(db)
    }
    fun createUser(phone: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("phone", phone)
        }
        return db.insert("user", null, values)
    }
    fun createUserInstruction(
        phone: String,
        instructionName: String,
        mediaPath: String,
        instructionPath: String,
        ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("phone", phone)
            put("instruction_name", instructionName)
            put("media_path", mediaPath)
            put("instruction_path", instructionPath)

        }
        Log.d("SQL","instruction_name")
        return db.insert("user_instruction", null, values)
    }
    fun getUserInstructions(phone: String): List<UserInstruction> {
        val db = writableDatabase
        val instructions = mutableListOf<UserInstruction>()

        val cursor = db.query(
            "user_instruction",
            null,
            "phone = ?",
            arrayOf(phone),
            null,
            null,
            null
        )

        with(cursor) {
            while (moveToNext()) {
                instructions.add(
                    UserInstruction(
                        phone = getString(getColumnIndexOrThrow("phone")),
                        instructionName=getString(getColumnIndexOrThrow("instruction_name"))
                    )
                )
            }
            close()
        }

        return instructions
    }
    companion object {
        private const val DATABASE_NAME = "app_database.db"
        private const val DATABASE_VERSION = 1
    }
}