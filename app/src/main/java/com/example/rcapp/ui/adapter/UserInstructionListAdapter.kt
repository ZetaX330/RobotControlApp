package com.example.rcapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rcapp.R
import com.example.rcapp.databinding.ItemListUserInstructionBinding
import com.example.rcapp.model.UserInstruction
import com.example.rcapp.ui.activity.CameraActivity
import com.example.rcapp.ui.activity.UserInstructionActivity

class UserInstructionListAdapter(
    private val context: Context,
    private val dataList:List<UserInstruction>,
    private val exeInsAdapter:UserInstructionExecutionAdapter
    ):RecyclerView.Adapter<UserInstructionListAdapter.ViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_user_instruction, parent, false)
        // 使用View Binding替代findViewById
        val inflater = LayoutInflater.from(context)
        val binding = ItemListUserInstructionBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.userInstructionNameTv.text=dataList[position].instructionName
        holder.binding.userInstructionExecutionAddIv.setOnClickListener {
            exeInsAdapter.addExeIns()
        }
    }

    override fun getItemCount(): Int = dataList.size

    // ViewHolder继承自父类RecyclerView.ViewHolder
    class ViewHolder
    // 构造函数，接受一个 Binding参数
    internal constructor(val binding: ItemListUserInstructionBinding) :
        RecyclerView.ViewHolder(binding.root)
}
