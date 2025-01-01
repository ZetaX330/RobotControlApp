package com.example.rcapp.adapter

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rcapp.databinding.InsSendItemLayoutBinding
import com.example.rcapp.model.InstructionSend

class InsSendListAdapter(private val context: Context?) :
    RecyclerView.Adapter<InsSendListAdapter.ViewHolder>() {
    private val insSendList: MutableList<InstructionSend> =
        ArrayList()


    fun addIns(insSend: InstructionSend) {
        if (!insSendList.contains(insSend)) {
            insSendList.add(insSend)
            if (insSendList.size > 200) {
                insSendList.subList(0, 100).clear()
                notifyItemRangeRemoved(0, 100)
            } else {
                notifyItemInserted(insSendList.size - 1)
            }
        }
    }

    override fun getItemCount(): Int {
        return insSendList.size
    }

    private fun getItem(position: Int): InstructionSend {
        return insSendList[position]
    }

    // 创建并返回一个新的ViewHolder实例
    // parent即为RecyclerView，viewType为RecyclerView布局类型
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 使用View Binding替代findViewById
        val inflater = LayoutInflater.from(context)
        val binding = InsSendItemLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    // 将数据绑定到指定位置的ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val insSend = getItem(position)
        holder.binding.insSendTv.text = insSend.instruction
        holder.binding.insSendTimeTv.text=insSend.time
        holder.binding.executePendingBindings()
    }

    // ViewHolder继承自父类RecyclerView.ViewHolder
    class ViewHolder// super用于调用父类RecyclerView.ViewHolder的构造函数，将视图传递给它
    //设置binding
    // 构造函数，接受一个 Binding参数
    internal constructor(val binding: InsSendItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)
}