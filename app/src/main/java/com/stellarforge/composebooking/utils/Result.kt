package com.stellarforge.composebooking.utils

/**
 * Asenkron işlemlerin veya veri getirme operasyonlarının sonucunu temsil eden
 * genel amaçlı bir sealed class.
 *
 * @param T Başarılı durumda sarmalanacak verinin türü.
 */
sealed class Result<out T> {
    /**
     * İşlemin başarıyla tamamlandığını ve veri içerdiğini belirtir.
     * @param data İşlem sonucu elde edilen veri.
     */
    data class Success<out T>(val data: T) : Result<T>()


    /**
     * İşlem sırasında bir hata oluştuğunu belirtir.
     * @param exception Oluşan istisna (exception).
     * @param message Opsiyonel, kullanıcıya gösterilebilecek veya loglanabilecek bir hata mesajı.
     *                Varsayılan olarak exception'ın localizedMessage'ını alır.
     */
    data class Error(
        val exception: Exception,
        val message: String? = exception.localizedMessage
    ) : Result<Nothing>()


    /**
     * İşlemin halen devam ettiğini, verinin yüklendiğini belirtir (opsiyonel).
     */
    object Loading : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=${exception::class.simpleName}, message=$message]"
            Loading -> "Loading"
        }
    }
}

inline fun <T, R> Result<T>.mapOnSuccess(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this // Artık kendi Error tipimizle uyumlu
        is Result.Loading -> Result.Loading // Artık kendi Loading tipimizle uyumlu
    }
}

inline fun <T> Result<T>.onSuccess(action: (data: T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

inline fun <T> Result<T>.onError(action: (exception: Exception, message: String?) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception, message)
    }
    return this
}

inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}

