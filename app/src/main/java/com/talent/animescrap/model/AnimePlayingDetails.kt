package com.talent.animescrap.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AnimePlayingDetails(
    val animeName: String,
    val animeUrl: String,
    val animeEpisodeIndex: String,
    val animeEpisodeMap: HashMap<String, String>,
    val animeTotalEpisode: String,
    val epType: String
) : Parcelable