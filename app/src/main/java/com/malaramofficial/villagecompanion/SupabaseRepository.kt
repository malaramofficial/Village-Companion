package com.malaramofficial.villagecompanion

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseServiceRow(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val emoji: String? = null,
    val sort_order: Int = 0,
    val active: Boolean = true
)

@Serializable
data class DistrictRow(val id: String, val name: String)

@Serializable
data class BlockRow(val id: String, val district_id: String, val name: String)

@Serializable
data class GramPanchayatRow(val id: String, val block_id: String, val name: String)

@Serializable
data class VillageRow(
    val id: String,
    val name: String,
    val block_id: String? = null,
    val gram_panchayat_id: String? = null
)

private const val LOCATION_TIMEOUT_MS = 10_000L

/** Live read-only location/service data for the V1 public catalogue. */
object SupabaseRepository {
    suspend fun getActiveServices(): List<SupabaseServiceRow> =
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("services").select(
                columns = Columns.list("id", "name", "subtitle", "emoji", "sort_order", "active")
            ) {
                filter { eq("active", true) }
            }.decodeList<SupabaseServiceRow>().sortedBy { it.sort_order }
        }

    suspend fun getDistrict(name: String): DistrictRow? =
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("districts").select(
                columns = Columns.list("id", "name")
            ) {
                filter { eq("name", name) }
            }.decodeList<DistrictRow>().firstOrNull()
        }

    suspend fun getBlocks(districtId: String): List<BlockRow> =
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("blocks").select(
                columns = Columns.list("id", "district_id", "name")
            ) {
                filter { eq("district_id", districtId) }
            }.decodeList<BlockRow>().sortedBy { it.name }
        }

    suspend fun getGramPanchayats(blockId: String): List<GramPanchayatRow> =
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("gram_panchayats").select(
                columns = Columns.list("id", "block_id", "name")
            ) {
                filter { eq("block_id", blockId) }
            }.decodeList<GramPanchayatRow>().sortedBy { it.name }
        }

    suspend fun getVillages(gramPanchayatId: String): List<VillageRow> =
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("villages").select(
                columns = Columns.list("id", "name", "block_id", "gram_panchayat_id")
            ) {
                filter { eq("gram_panchayat_id", gramPanchayatId) }
            }.decodeList<VillageRow>().sortedBy { it.name }
        }
}
