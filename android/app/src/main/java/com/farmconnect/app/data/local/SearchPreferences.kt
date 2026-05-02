package com.farmconnect.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "search_prefs",
        Context.MODE_PRIVATE
    )

    fun saveSearchQuery(query: String) {
        prefs.edit().putString(KEY_SEARCH_QUERY, query).apply()
    }

    fun getSearchQuery(): String {
        return prefs.getString(KEY_SEARCH_QUERY, "") ?: ""
    }

    fun saveFilters(
        variety: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minQuantity: Double? = null,
        maxQuantity: Double? = null,
        qualityGrade: String? = null,
        city: String? = null,
        state: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null
    ) {
        prefs.edit().apply {
            putString(KEY_VARIETY, variety)
            putFloat(KEY_MIN_PRICE, minPrice?.toFloat() ?: -1f)
            putFloat(KEY_MAX_PRICE, maxPrice?.toFloat() ?: -1f)
            putFloat(KEY_MIN_QUANTITY, minQuantity?.toFloat() ?: -1f)
            putFloat(KEY_MAX_QUANTITY, maxQuantity?.toFloat() ?: -1f)
            putString(KEY_QUALITY_GRADE, qualityGrade)
            putString(KEY_CITY, city)
            putString(KEY_STATE, state)
            putString(KEY_SORT_BY, sortBy)
            putString(KEY_SORT_ORDER, sortOrder)
            apply()
        }
    }

    fun getSavedFilters(): SearchFilters {
        return SearchFilters(
            variety = prefs.getString(KEY_VARIETY, null),
            minPrice = prefs.getFloat(KEY_MIN_PRICE, -1f).let { if (it == -1f) null else it.toDouble() },
            maxPrice = prefs.getFloat(KEY_MAX_PRICE, -1f).let { if (it == -1f) null else it.toDouble() },
            minQuantity = prefs.getFloat(KEY_MIN_QUANTITY, -1f).let { if (it == -1f) null else it.toDouble() },
            maxQuantity = prefs.getFloat(KEY_MAX_QUANTITY, -1f).let { if (it == -1f) null else it.toDouble() },
            qualityGrade = prefs.getString(KEY_QUALITY_GRADE, null),
            city = prefs.getString(KEY_CITY, null),
            state = prefs.getString(KEY_STATE, null),
            sortBy = prefs.getString(KEY_SORT_BY, "date"),
            sortOrder = prefs.getString(KEY_SORT_ORDER, "desc")
        )
    }

    fun clearFilters() {
        prefs.edit().apply {
            remove(KEY_VARIETY)
            remove(KEY_MIN_PRICE)
            remove(KEY_MAX_PRICE)
            remove(KEY_MIN_QUANTITY)
            remove(KEY_MAX_QUANTITY)
            remove(KEY_QUALITY_GRADE)
            remove(KEY_CITY)
            remove(KEY_STATE)
            remove(KEY_SORT_BY)
            remove(KEY_SORT_ORDER)
            apply()
        }
    }

    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_VARIETY = "variety"
        private const val KEY_MIN_PRICE = "min_price"
        private const val KEY_MAX_PRICE = "max_price"
        private const val KEY_MIN_QUANTITY = "min_quantity"
        private const val KEY_MAX_QUANTITY = "max_quantity"
        private const val KEY_QUALITY_GRADE = "quality_grade"
        private const val KEY_CITY = "city"
        private const val KEY_STATE = "state"
        private const val KEY_SORT_BY = "sort_by"
        private const val KEY_SORT_ORDER = "sort_order"
    }
}

data class SearchFilters(
    val variety: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minQuantity: Double? = null,
    val maxQuantity: Double? = null,
    val qualityGrade: String? = null,
    val city: String? = null,
    val state: String? = null,
    val sortBy: String? = "date",
    val sortOrder: String? = "desc"
)
