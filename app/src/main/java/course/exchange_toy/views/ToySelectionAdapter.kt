package course.exchange_toy.views

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import course.exchange_toy.databinding.ItemToyBinding
import course.exchange_toy.models.remote.models.Toy
import course.exchange_toy.models.remote.models.ToyStatus

/**
 * Адаптер для выбора игрушки (используется в диалоге)
 * Использует тот же layout что и обычный список, но с выделением выбранной
 */
class ToySelectionAdapter(
    private val onToySelected: (Toy) -> Unit
) : ListAdapter<Toy, ToySelectionAdapter.ViewHolder>(DiffCallback()) {
    
    private var selectedPosition = -1
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemToyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val toy = getItem(position)
        holder.bind(toy, position == selectedPosition) {
            val oldPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onToySelected(toy)
        }
    }
    
    fun getSelectedToy(): Toy? {
        return if (selectedPosition >= 0 && selectedPosition < currentList.size) {
            getItem(selectedPosition)
        } else null
    }
    
    class ViewHolder(
        private val binding: ItemToyBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(toy: Toy, isSelected: Boolean, onClick: () -> Unit) {
            binding.tvToyName.text = toy.name
            
            // Статус с цветовым индикатором
            val (statusText, statusColor) = when (toy.status) {
                ToyStatus.CREATED -> "Создана" to Color.parseColor("#4CAF50") // Зеленый
                ToyStatus.EXCHANGING -> "На обмене" to Color.parseColor("#FF9800") // Оранжевый
                ToyStatus.EXCHANGED -> "Обменяна" to Color.parseColor("#2196F3") // Синий
                ToyStatus.REMOVED -> "Удалена" to Color.parseColor("#F44336") // Красный
            }
            binding.tvToyStatus.text = "Статус: $statusText"
            binding.statusIndicator.setBackgroundColor(statusColor)
            
            // Визуальное выделение выбранной игрушки
            if (isSelected) {
                itemView.setBackgroundColor(Color.parseColor("#E3F2FD")) // Светло-голубой фон
                binding.btnDetails.text = "✓ Выбрано"
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
                binding.btnDetails.text = "Выбрать"
            }
            
            // Клик по всему элементу
            itemView.setOnClickListener { onClick() }
            binding.btnDetails.setOnClickListener { onClick() }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Toy>() {
        override fun areItemsTheSame(oldItem: Toy, newItem: Toy) = 
            oldItem.toyId == newItem.toyId
        override fun areContentsTheSame(oldItem: Toy, newItem: Toy) = 
            oldItem == newItem
    }
}

