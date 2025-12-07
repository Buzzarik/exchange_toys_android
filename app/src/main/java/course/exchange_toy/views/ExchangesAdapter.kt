package course.exchange_toy.views

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import course.exchange_toy.databinding.ItemExchangeBinding
import course.exchange_toy.models.remote.models.ExchangeInfo
import course.exchange_toy.models.remote.models.ExchangeStatus

/**
 * Адаптер для списка обменов
 */
class ExchangesAdapter(
    private val onDetailsClick: (ExchangeInfo) -> Unit
) : ListAdapter<ExchangeInfo, ExchangesAdapter.ExchangeViewHolder>(ExchangeDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeViewHolder {
        val binding = ItemExchangeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExchangeViewHolder(binding, onDetailsClick)
    }
    
    override fun onBindViewHolder(holder: ExchangeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ExchangeViewHolder(
        private val binding: ItemExchangeBinding,
        private val onDetailsClick: (ExchangeInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(exchange: ExchangeInfo) {
            // Получаем обе игрушки из деталей обмена
            val toy1 = exchange.details.getOrNull(0)
            val toy2 = exchange.details.getOrNull(1)
            
            val exchangeTitle = "${toy1?.toy?.name ?: "?"} ⇄ ${toy2?.toy?.name ?: "?"}"
            binding.tvExchangeTitle.text = exchangeTitle
            
            // Статус обмена с цветовым индикатором
            val (statusText, statusColor) = when (exchange.status) {
                ExchangeStatus.CREATED -> "Ожидает действий" to Color.parseColor("#2196F3") // 🔵 Синий
                ExchangeStatus.CONFIRM -> "Обе стороны готовы" to Color.parseColor("#FF9800") // 🟠 Оранжевый
                ExchangeStatus.SUCCESS -> "Завершен" to Color.parseColor("#4CAF50") // 🟢 Зеленый
                ExchangeStatus.FAILED -> "Отменен" to Color.parseColor("#F44336") // 🔴 Красный
            }
            binding.tvExchangeStatus.text = "Статус: $statusText"
            binding.statusIndicator.setBackgroundColor(statusColor)
            
            // Кнопка "Детали"
            binding.btnDetails.setOnClickListener {
                onDetailsClick(exchange)
            }
        }
    }
    
    class ExchangeDiffCallback : DiffUtil.ItemCallback<ExchangeInfo>() {
        override fun areItemsTheSame(oldItem: ExchangeInfo, newItem: ExchangeInfo): Boolean {
            return oldItem.exchangeId == newItem.exchangeId
        }
        
        override fun areContentsTheSame(oldItem: ExchangeInfo, newItem: ExchangeInfo): Boolean {
            // Сравниваем статусы и детали обмена
            if (oldItem.status != newItem.status) return false
            if (oldItem.details.size != newItem.details.size) return false
            
            // Проверяем каждую деталь (обычно 2 элемента)
            return oldItem.details.zip(newItem.details).all { (old, new) ->
                old.status == new.status &&
                old.toy.toyId == new.toy.toyId &&
                old.toy.name == new.toy.name
            }
        }
    }
}

