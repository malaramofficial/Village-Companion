package com.malaramofficial.villagecompanion

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase configuration for the Android client.
 *
 * The publishable key is designed for client applications. Database access is
 * still controlled by Supabase Row Level Security policies.
 * Never put a Supabase secret/service key in this file or in the APK.
 */
object SupabaseConfig {
    const val URL = "https://vpbojhvxlduqiuqpymys.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_cC5lyJ3bKlrXA0l_zKm90Q_u4LOttJc"
}

val supabase = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.PUBLISHABLE_KEY
) {
    install(Postgrest)
    install(Auth)
}
