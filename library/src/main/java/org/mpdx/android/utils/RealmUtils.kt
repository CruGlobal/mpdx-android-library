package org.mpdx.android.utils

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import io.realm.Realm
import io.realm.RealmAsyncTask
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.kotlin.isManaged
import org.mpdx.android.base.model.UniqueItem
import timber.log.Timber

private const val TAG = "RealmUtils"

inline val RealmModel.realm: Realm? get() = RealmObject.getRealm(this)

@WorkerThread
fun Realm.useInTransaction(transaction: Realm.() -> Unit) =
    use { executeTransaction(transaction) }

@AnyThread
inline fun Realm.useInTransactionAsync(
    onSuccess: Realm.Transaction.OnSuccess? = null,
    onError: Realm.Transaction.OnError? = null,
    crossinline transaction: Realm.() -> Unit
): RealmAsyncTask = use { executeTransactionAsync({ it.transaction() }, onSuccess, onError) }

@WorkerThread
fun realmTransaction(transaction: Realm.() -> Unit) =
    Realm.getDefaultInstance().useInTransaction(transaction)

@AnyThread
internal inline fun realmTransactionAsync(crossinline transaction: Realm.() -> Unit) =
    realmTransactionAsync(onSuccess = null, transaction = transaction)

@MainThread
internal inline fun realmTransactionAsync(
    crossinline onSuccess: () -> Unit,
    crossinline transaction: Realm.() -> Unit
) = realmTransactionAsync(onSuccess = Realm.Transaction.OnSuccess { onSuccess() }, transaction = transaction)

@MainThread
internal inline fun realmTransactionAsync(
    crossinline onError: (Throwable) -> Unit,
    crossinline transaction: Realm.() -> Unit
) = realmTransactionAsync(onError = Realm.Transaction.OnError { onError(it) }, transaction = transaction)

@MainThread
internal inline fun realmTransactionAsync(
    crossinline onSuccess: () -> Unit,
    crossinline onError: (Throwable) -> Unit,
    crossinline transaction: Realm.() -> Unit
) = realmTransactionAsync(
    onSuccess = Realm.Transaction.OnSuccess { onSuccess() },
    onError = Realm.Transaction.OnError { onError(it) },
    transaction = transaction
)

@AnyThread
inline fun realmTransactionAsync(
    onSuccess: Realm.Transaction.OnSuccess? = null,
    onError: Realm.Transaction.OnError? = null,
    crossinline transaction: Realm.() -> Unit
): RealmAsyncTask = Realm.getDefaultInstance().useInTransactionAsync(onSuccess, onError, transaction)

inline fun <reified T> realm(block: Realm.() -> T): T = Realm.getDefaultInstance().use(block)

inline fun <reified T> Realm.getManagedVersionOrNull(obj: T) where T : RealmObject, T : UniqueItem = when {
    obj.isManaged -> obj
    else -> where(T::class.java).equalTo(UniqueItem.FIELD_ID, obj.id).findFirst()
}

/**
 * @param block function that merges a model with an existing managed model.
 */
@Suppress("UNCHECKED_CAST")
fun <E : RealmModel> Realm.mergeWithExistingModel(
    model: E,
    maxDepth: Int = 3,
    block: (model: RealmModel, existing: RealmModel) -> RealmModel
): E {
    // There is no need to process managed models, they are already the existing version...
    if (model.isManaged()) return model

    try {
        val clazz = model.javaClass

        // merge model with the existing version
        val existing =
            if (model is UniqueItem) where(clazz).equalTo(UniqueItem.FIELD_ID, model.id).findFirst() else null
        val merged = existing?.let { block(model, existing) as E } ?: model

        // if the merged object isn't the pre-existing model, recursively merge any nested models
        if (merged !== existing && maxDepth > 0) {
            // recurse into nested attributes to find placeholders
            clazz.declaredFields.forEach { field ->
                field.isAccessible = true
                when (val value = field.get(merged)) {
                    is RealmModel -> field.set(merged, mergeWithExistingModel(value, maxDepth - 1, block))
                    is MutableList<*> -> {
                        val values: MutableListIterator<Any?> = value.listIterator() as MutableListIterator<Any?>
                        while (values.hasNext()) {
                            values.next()
                                ?.takeIf { it is RealmModel }
                                ?.let { values.set(mergeWithExistingModel(it as RealmModel, maxDepth - 1, block)) }
                        }
                    }
                }
            }
        }

        // return the merged version
        return merged
    } catch (e: Exception) {
        Timber.tag(TAG).e(e)
    }

    // return the original object
    return model
}

fun <E : RealmModel> E.copyFromRealm(maxDepth: Int = Integer.MAX_VALUE): E = realm!!.copyFromRealm(this, maxDepth)
fun <E : RealmModel> RealmList<E>.copyFromRealm(maxDepth: Int = Integer.MAX_VALUE): List<E> =
    realm.copyFromRealm(this, maxDepth)

fun <E : RealmModel> RealmResults<E>.copyFromRealm(maxDepth: Int = Integer.MAX_VALUE): List<E> =
    realm.copyFromRealm(this, maxDepth)

fun <E : Any?> Collection<E>.toRealmList() = RealmList<E>().also { it.addAll(this) }
