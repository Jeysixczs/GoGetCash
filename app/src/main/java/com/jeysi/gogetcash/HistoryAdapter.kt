package com.jeysi.gogetcash

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var transactions: List<AnyTransaction>,
    private val onItemClicked: (AnyTransaction) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvLoanerName) // Reusing the layout's ID
        val tvDate: TextView = view.findViewById(R.id.tvTransactionDate) // Reusing the layout's ID
        val tvAmount: TextView = view.findViewById(R.id.tvLoanAmount) // Reusing the layout's ID
        val tvType: TextView = view.findViewById(R.id.tvFee) // Reusing the fee TextView for type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        // We can reuse the item_transaction.xml layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context

        holder.tvName.text = transaction.name
        holder.tvDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(transaction.timestamp))
        holder.tvAmount.text = String.format("₱%.2f", transaction.amount)

        when (transaction.type) {
            "Loan" -> {
                holder.tvType.text = "Loan"
                if (transaction.isPaid == true) {
                    holder.tvAmount.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                } else {
                    holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.red))
                }
            }
            "Cash In" -> {
                holder.tvType.text = "Cash In"
                holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green))
            }
            "Cash Out" -> {
                holder.tvType.text = "Cash Out"
                holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.primaryColor))
            }
            else -> {
                holder.tvType.text = "Transaction"
                holder.tvAmount.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            }
        }

        holder.itemView.setOnClickListener {
            onItemClicked(transaction)
        }
    }

    override fun getItemCount() = transactions.size
}
