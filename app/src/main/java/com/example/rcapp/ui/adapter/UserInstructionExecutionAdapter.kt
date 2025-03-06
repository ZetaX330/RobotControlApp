package com.example.rcapp.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rcapp.R
import com.example.rcapp.databinding.ItemListUserExecutionInstructionBinding
import java.util.Locale

class UserInstructionExecutionAdapter(
    private val context: Context,
    private val dataList: MutableList<Int> =mutableListOf()) :
    RecyclerView.Adapter<UserInstructionExecutionAdapter.ViewHolder>() {
    fun getInsList():MutableList<Int>{
        return  dataList
    }
    fun addExeIns(){
        dataList.add(dataList.size)
        notifyItemInserted((dataList.size - 1))
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_user_execution_instruction, parent, false)
        // 使用View Binding替代findViewById
        val inflater = LayoutInflater.from(context)
        val binding = ItemListUserExecutionInstructionBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.userInstructionExecutionTv.text=String.format(Locale.CHINA,"%d",dataList[position])
        holder.binding.userInstructionExecutionRemoveIv.setOnClickListener {
            // ✅ 使用动态更新的 bindingAdapterPosition
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                dataList.removeAt(currentPosition)
                notifyItemRemoved(currentPosition)
                notifyItemRangeChanged(currentPosition, dataList.size - currentPosition)
            }

        }
    }

    override fun getItemCount(): Int = dataList.size

    // ViewHolder继承自父类RecyclerView.ViewHolder
    class ViewHolder
    // 构造函数，接受一个 Binding参数
    internal constructor(val binding: ItemListUserExecutionInstructionBinding) :
        RecyclerView.ViewHolder(binding.root)
}