package nl.neerdael.backdrop_launcher.core

data class HomeData(val scene: Scene, val providerStatus: String, val backdropTitle: String? = null,
    val backdropDescription: String? = null, val watchNextPermissionGranted: Boolean = true,
    val recommendations: List<ResumeItem> = emptyList())
