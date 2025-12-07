package course.exchange_toy.utils

import android.widget.Toast
import androidx.fragment.app.Fragment
import course.exchange_toy.models.remote.ExchangeToysClient
import course.exchange_toy.models.user.UserSession

/**
 * Extension функции для упрощения повторяющегося кода
 */

/**
 * Показать короткое Toast сообщение
 */
fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

/**
 * Показать длинное Toast сообщение (для ошибок)
 */
fun Fragment.showLongToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
}

/**
 * Создать API клиент на основе сохраненного хоста
 */
fun UserSession.createApiClient(): ExchangeToysClient {
    return ExchangeToysClient(getApiHost())
}

