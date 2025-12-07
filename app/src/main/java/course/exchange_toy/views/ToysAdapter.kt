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
 * Адаптер для списка игрушек
 */
class ToysAdapter(
    private val onDetailsClick: (Toy) -> Unit
) : ListAdapter<Toy, ToysAdapter.ToyViewHolder>(ToyDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToyViewHolder {
        val binding = ItemToyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ToyViewHolder(binding, onDetailsClick)
    }
    
    override fun onBindViewHolder(holder: ToyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ToyViewHolder(
        private val binding: ItemToyBinding,
        private val onDetailsClick: (Toy) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(toy: Toy) {
            binding.tvToyName.text = toy.name
            
            // Отображаем статус и устанавливаем цвет индикатора
            val (statusText, statusColor) = when (toy.status) {
                ToyStatus.CREATED -> "Создана" to Color.parseColor("#4CAF50") // Зеленый
                ToyStatus.EXCHANGING -> "На обмене" to Color.parseColor("#FF9800") // Оранжевый
                ToyStatus.EXCHANGED -> "Обменяна" to Color.parseColor("#2196F3") // Синий
                ToyStatus.REMOVED -> "Удалена" to Color.parseColor("#F44336") // Красный
            }
            binding.tvToyStatus.text = "Статус: $statusText"
            binding.statusIndicator.setBackgroundColor(statusColor)
            
            // Кнопка "Детали"
            binding.btnDetails.setOnClickListener {
                onDetailsClick(toy)
            }
        }
    }
    
    class ToyDiffCallback : DiffUtil.ItemCallback<Toy>() {
        override fun areItemsTheSame(oldItem: Toy, newItem: Toy): Boolean {
            // Сравниваем по уникальному ID
            return oldItem.toyId == newItem.toyId
        }
        
        override fun areContentsTheSame(oldItem: Toy, newItem: Toy): Boolean {
            // Сравниваем все поля, включая photoUrl
            return oldItem.name == newItem.name &&
                   oldItem.status == newItem.status &&
                   oldItem.description == newItem.description &&
                   oldItem.photoUrl == newItem.photoUrl
        }
    }
}

