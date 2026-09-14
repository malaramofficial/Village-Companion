package com.malaramofficial.villagecompanion

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseServiceRow(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val emoji: String? = null,
    val sort_order: Int = 0,
    val is_active: Boolean = true
)

/**
 * Small data layer kept separate from Compose UI.
 * The first live call reads the public V1 service catalogue.
 */
object SupabaseRepository {
    suspend fun getActiveServices(): List<SupabaseServiceRow> {
        return supabase
            .from("services")
            .select {
                filter {
                    eq("is_active", true)
                }
            }
            .decodeList<SupabaseServiceRow>()
            .sortedBy { it.sort_order }
    }
}
