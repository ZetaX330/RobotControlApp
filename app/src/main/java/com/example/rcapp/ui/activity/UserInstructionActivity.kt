package com.example.rcapp.ui.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rcapp.data.database.UserDatabaseHelper
import com.example.rcapp.databinding.ActivityUserInstructionBinding
import com.example.rcapp.model.UserInstruction
import com.example.rcapp.ui.adapter.UserInstructionItemDragAdapter
import com.example.rcapp.ui.adapter.UserInstructionListAdapter
import com.example.rcapp.ui.adapter.UserInstructionExecutionAdapter
import com.google.android.material.snackbar.Snackbar

class UserInstructionActivity : AppCompatActivity() {
    private lateinit var binding:ActivityUserInstructionBinding
    private lateinit var adapter: UserInstructionExecutionAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityUserInstructionBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialToolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)
        setSupportActionBar(binding.materialToolbar)
        supportActionBar?.apply {
            title = "用户指令"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.userInstructionExecutionRv.layoutManager = LinearLayoutManager(this)

        val yourDataList = mutableListOf(
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8
        )
        adapter = UserInstructionExecutionAdapter(this,yourDataList)
        binding.userInstructionExecutionRv.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(UserInstructionItemDragAdapter(yourDataList,adapter))
        itemTouchHelper.attachToRecyclerView(binding.userInstructionExecutionRv)

        binding.userInstructionRv.layoutManager=LinearLayoutManager(this)

        val sharedPreferences=getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.getString("phone",null)?.
        let {
            val dataList = UserDatabaseHelper(this).getUserInstructions(it)
            binding.userInstructionRv.adapter=UserInstructionListAdapter(this,dataList,adapter)
        }
        binding.userInstructionExecutionRosBtn.setOnClickListener {
            Snackbar.make(binding.root,adapter.getInsList().toString(),Snackbar.LENGTH_LONG).show()
        }
//        binding.userInstructionSwitchIv.apply {
//            setOnClickListener {
//                isSelected = !isSelected
//            }
//        }

    }


}