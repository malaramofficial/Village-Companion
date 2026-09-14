package com.malaramofficial.villagecompanion

/**
 * Bundled location catalogue used as the first/offline fallback.
 * Supabase remains the authoritative source whenever it is reachable.
 */
object LocalLocationData {
    const val districtName = "Barmer"

    data class Gp(val name: String, val villages: List<String> = emptyList())
    data class Block(val name: String, val gps: List<Gp>)

    val blocks = listOf(
        Block("Aadel", listOf(
            Gp("Aasuon Ki Dhani", listOf("Aasuon Ki Dhani", "Bhabhuon Ki Beri", "Dhelani Nadi", "Godaron Ki Beri")),
            Gp("Adarsh Adel"),
            Gp("Arjun Ki Dhani", listOf("Adarsh Chhotu", "Arjun Ki Dhani", "Baldev Nagar", "Bhomani Kadwasaron Ki Dhani", "Ed Chhotu", "Khaniya", "Navlasar", "Peerani Godaron Ki Dhani", "Udai Nagar", "Vijai Nagar")),
            Gp("Bhambhu Nagar"),
            Gp("Dholpaliya Nada", listOf("Bhilon Ka Gol", "Dholpaliya Nada", "Laxmannagar", "Siddhoniyo Khotho Ki Dhani")),
            Gp("Khardi Beri", listOf("Aadhtara", "Adel Panji", "Adel Shivnagar @ Peme Ki Beri", "Bateron Ki Beri", "Jethasar", "Kanani Dhakon Ki Dhani", "Khardi Beri", "Sadul Nagar")),
            Gp("Khariya Khurd", listOf("Bananiyon Ki Beri", "Bhurasar", "Choraliya Nada", "Dhannani Kumharo Ki Dhani", "Gangapura", "Khariya Khurd", "Khubad Mata Mandir", "Kishanpura", "Meghwalon Ki Dhani", "Tarad Nagar")),
            Gp("Meethi Beri", listOf("Bakani Sarnon Ki Dhani", "Daukiyon Ki Dhani", "Kookano Ki Dhaniyan", "Lohamroad And Nanon Ki Dhani", "Meethi Beri", "Rajnagar", "Sarnon And Sewaron Ki Dhani", "Thoriyon Ki Dhani", "Visvakarma Nagar")),
            Gp("Sadecha", listOf("Budhrani Hudon Ki Dhani", "Karminagar", "Panani Dhatarwalon Ki Dhani", "Rupnagar", "Sadecha", "Shivnagar Sadecha", "Utam Nagar")),
            Gp("अणखिया"), Gp("आडेल"),
            Gp("छोटु", listOf("Behanoni Saranon Ki Dhani", "Chhotu", "Gogaji Ka Mandir Chhotu", "Jhardasar", "Jhurdo Ki Dhani", "Mahadeo Mandir", "Pokrasar", "Sanwalsar")),
            Gp("धोलानाडा"),
            Gp("निम्बलकोट", listOf("Bheraram Moondh Nagar", "Doloni Siyago Ka Tala", "Lakhoni Godaron Ki Dhani", "Lakhoni Megwalon Ki Dhani", "Neembal Kot", "Neembal Nadi", "Siyagon Ki Dhani Chak No1")),
            Gp("नोखड़ा", listOf("Adarsh Nokhra", "Bhomani Meghwalon Ki Dhani", "Guruon Ka Tala", "Hira Nagar", "Jagram Ki Dhani", "N.T. Nagar", "Nehron Ka Tala", "Nokhra", "Salgasar")),
            Gp("बाण्ड"),
            Gp("मालपुरा", listOf("Heerpura", "Hukmani Khoton Ki Dhani", "Malpura", "Thoriyon Ka Tala")),
            Gp("मंगले की बेरी", listOf("Ambedkar Nagar", "Dhanne Bhil Ki Dhani", "Gadher Magwalo Ki Dhani", "Khumoni Beniwalon Ki Dhani", "Mangle Ki Beri", "Naya Kua", "Radon And Kumharo Ki Dhani", "Ramnagar", "Tejasar", "Vagoni Dhatarwalon Ki Dhani", "Wankalsar")),
            Gp("राणासर खुर्द")
        )),
        Block("Barmer", emptyList()),
        Block("Barmer Rural", emptyList()),
        Block("Baytoo", emptyList()),
        Block("Chohtan", emptyList()),
        Block("Dhanau", emptyList()),
        Block("Fagliya", emptyList()),
        Block("Gadra Road", emptyList()),
        Block("Ramsar", emptyList()),
        Block("Sedwa", emptyList()),
        Block("Sheo", emptyList())
    )

    fun blockId(name: String) = "local-block-${name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}"
    fun gpId(block: String, gp: String) = "local-gp-${blockId(block)}-${gp.hashCode()}"
    fun villageId(block: String, gp: String, village: String) = "local-village-${blockId(block)}-${gp.hashCode()}-${village.hashCode()}"
}
