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

@Serializable
data class SupabaseProviderRow(
    val id: String,
    val profile_id: String,
    val service_id: String,
    val village_id: String,
    val phone: String,
    val availability: String = "available_now",
    val active: Boolean = true
)

@Serializable
data class SupabaseProviderActiveUpdate(val active: Boolean)

@Serializable
data class SupabaseProviderWrite(
    val profile_id: String,
    val service_id: String,
    val village_id: String,
    val phone: String,
    val availability: String = "available_now",
    val active: Boolean = true
)

private const val LOCATION_TIMEOUT_MS = 2_500L

/**
 * Data access layer.
 *
 * Location hierarchy is offline-first: the bundled catalogue is always available
 * as a fallback, while Supabase remains the authoritative source whenever it is
 * reachable and returns usable data.
 */
object SupabaseRepository {
    suspend fun getActiveProviders(serviceId: String, villageId: String): List<SupabaseProviderRow> =
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("providers").select(
                columns = Columns.list(
                    "id", "profile_id", "service_id", "village_id",
                    "phone", "availability", "active"
                )
            ) {
                filter {
                    eq("service_id", serviceId)
                    eq("village_id", villageId)
                    eq("active", true)
                }
            }.decodeList<SupabaseProviderRow>()
        }

    suspend fun getMyProvider(profileId: String): SupabaseProviderRow? =
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("providers").select(
                columns = Columns.list(
                    "id", "profile_id", "service_id", "village_id",
                    "phone", "availability", "active"
                )
            ) {
                filter { eq("profile_id", profileId) }
            }.decodeList<SupabaseProviderRow>().firstOrNull()
        }

    suspend fun createProvider(provider: SupabaseProviderWrite) {
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("providers").insert(provider)
        }
    }

    suspend fun updateProvider(providerId: String, provider: SupabaseProviderWrite) {
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("providers").update(provider) {
                filter { eq("id", providerId) }
            }
        }
    }

    suspend fun deactivateProvider(providerId: String) {
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("providers").update(SupabaseProviderActiveUpdate(false)) {
                filter { eq("id", providerId) }
            }
        }
    }


    suspend fun getActiveServices(): List<SupabaseServiceRow> =
        withTimeout(LOCATION_TIMEOUT_MS) {
            supabase.from("services").select(
                columns = Columns.list("id", "name", "subtitle", "emoji", "sort_order", "active")
            ) {
                filter { eq("active", true) }
            }.decodeList<SupabaseServiceRow>().sortedBy { it.sort_order }
        }

    suspend fun getDistrict(name: String): DistrictRow? {
        val remote = runCatching {
            withTimeout(LOCATION_TIMEOUT_MS) {
                supabase.from("districts").select(
                    columns = Columns.list("id", "name")
                ) {
                    filter { eq("name", name) }
                }.decodeList<DistrictRow>().firstOrNull()
            }
        }.getOrNull()
        if (remote != null) return remote
        return if (name.equals(LocalLocationData.districtName, ignoreCase = true)) {
            DistrictRow("local-district-barmer", LocalLocationData.districtName)
        } else null
    }

    suspend fun getBlocks(districtId: String): List<BlockRow> {
        if (districtId.startsWith("local-")) return localBlocks()
        val remote = runCatching {
            withTimeout(LOCATION_TIMEOUT_MS) {
                supabase.from("blocks").select(
                    columns = Columns.list("id", "district_id", "name")
                ) {
                    filter { eq("district_id", districtId) }
                }.decodeList<BlockRow>().sortedBy { it.name }
            }
        }.getOrNull()
        return remote?.takeIf { it.isNotEmpty() } ?: localBlocks()
    }

    suspend fun getGramPanchayats(blockId: String): List<GramPanchayatRow> {
        if (blockId.startsWith("local-")) return localGps(blockId)
        val remote = runCatching {
            withTimeout(LOCATION_TIMEOUT_MS) {
                supabase.from("gram_panchayats").select(
                    columns = Columns.list("id", "block_id", "name")
                ) {
                    filter { eq("block_id", blockId) }
                }.decodeList<GramPanchayatRow>().sortedBy { it.name }
            }
        }.getOrNull()
        return remote?.takeIf { it.isNotEmpty() } ?: localGps(blockId)
    }

    suspend fun getVillages(gramPanchayatId: String): List<VillageRow> {
        if (gramPanchayatId.startsWith("local-")) return localVillages(gramPanchayatId)
        val remote = runCatching {
            withTimeout(LOCATION_TIMEOUT_MS) {
                supabase.from("villages").select(
                    columns = Columns.list("id", "name", "block_id", "gram_panchayat_id")
                ) {
                    filter { eq("gram_panchayat_id", gramPanchayatId) }
                }.decodeList<VillageRow>().sortedBy { it.name }
            }
        }.getOrNull()
        return remote?.takeIf { it.isNotEmpty() } ?: localVillages(gramPanchayatId)
    }

    private fun localBlocks(): List<BlockRow> =
        LocalLocationData.blocks.map {
            BlockRow(LocalLocationData.blockId(it.name), "local-district-barmer", it.name)
        }

    private fun localGps(blockId: String): List<GramPanchayatRow> {
        val block = LocalLocationData.blocks.firstOrNull { LocalLocationData.blockId(it.name) == blockId }
            ?: return emptyList()
        return block.gps.map {
            GramPanchayatRow(LocalLocationData.gpId(block.name, it.name), blockId, it.name)
        }.sortedBy { it.name }
    }

    private fun localVillages(gpId: String): List<VillageRow> {
        val match = LocalLocationData.blocks.asSequence()
            .flatMap { block -> block.gps.asSequence().map { block to it } }
            .firstOrNull { (block, gp) -> LocalLocationData.gpId(block.name, gp.name) == gpId }
            ?: return emptyList()
        val (block, gp) = match
        return gp.villages.map {
            VillageRow(
                id = LocalLocationData.villageId(block.name, gp.name, it),
                name = it,
                block_id = LocalLocationData.blockId(block.name),
                gram_panchayat_id = gpId
            )
        }.sortedBy { it.name }
    }
}
