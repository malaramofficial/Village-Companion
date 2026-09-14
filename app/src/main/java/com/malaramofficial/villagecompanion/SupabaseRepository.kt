package com.malaramofficial.villagecompanion

import io.github.jan.supabase.postgrest.from
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

/** Live read-only location/service data for the V1 public catalogue. */
object SupabaseRepository {
    suspend fun getActiveServices(): List<SupabaseServiceRow> =
        supabase.from("services").select {
            filter { eq("is_active", true) }
        }.decodeList<SupabaseServiceRow>().sortedBy { it.sort_order }

    suspend fun getDistrict(name: String): DistrictRow? =
        supabase.from("districts").select {
            filter { eq("name", name); eq("is_active", true) }
        }.decodeList<DistrictRow>().firstOrNull()

    suspend fun getBlocks(districtId: String): List<BlockRow> =
        supabase.from("blocks").select {
            filter { eq("district_id", districtId); eq("is_active", true) }
        }.decodeList<BlockRow>().sortedBy { it.name }

    suspend fun getGramPanchayats(blockId: String): List<GramPanchayatRow> =
        supabase.from("gram_panchayats").select {
            filter { eq("block_id", blockId); eq("is_active", true) }
        }.decodeList<GramPanchayatRow>().sortedBy { it.name }

    suspend fun getVillages(gramPanchayatId: String): List<VillageRow> =
        supabase.from("villages").select {
            filter { eq("gram_panchayat_id", gramPanchayatId); eq("is_active", true) }
        }.decodeList<VillageRow>().sortedBy { it.name }
}
