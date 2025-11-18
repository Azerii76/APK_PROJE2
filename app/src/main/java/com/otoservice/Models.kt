package com.otoservice

data class AutoReplyLog(
    val appName: String,
    val target: String,
    val timestamp: Long,
    val preview: String
)

object FrequencyDefaults {
    // Aynı sohbete tekrar yanıt göndermeden önce beklenecek süre seçenekleri
    val replySeconds = listOf(5, 15, 30, 60, 120)
    // Konum değişim aralığı seçenekleri
    val locationMinutes = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)
}

object LocationDefaults {
    const val RANDOM_DISTRICT = "Rastgele (İstanbul Geneli)"
    val districts = listOf(
        "Adalar", "Arnavutköy", "Ataşehir", "Avcılar", "Bağcılar", "Bahçelievler",
        "Bakırköy", "Başakşehir", "Bayrampaşa", "Beşiktaş", "Beykoz", "Beylikdüzü",
        "Beyoğlu", "Büyükçekmece", "Çatalca", "Çekmeköy", "Esenler", "Esenyurt",
        "Eyüpsultan", "Fatih", "Gaziosmanpaşa", "Güngören", "Kadıköy", "Kağıthane",
        "Kartal", "Küçükçekmece", "Maltepe", "Pendik", "Sancaktepe", "Sarıyer",
        "Silivri", "Sultanbeyli", "Sultangazi", "Şile", "Şişli", "Tuzla",
        "Ümraniye", "Üsküdar", "Zeytinburnu"
    )
}
