package com.talent.animescrap.ui.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.room.Room
import coil.load
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.talent.animescrap.R
import com.talent.animescrap.databinding.FragmentAnimeBinding
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.room.FavRoomModel
import com.talent.animescrap.room.LinksRoomDatabase
import com.talent.animescrap.ui.activities.PlayerActivity
import com.talent.animescrap.ui.viewmodels.AnimeDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AnimeFragment : Fragment() {


    private var _binding: FragmentAnimeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var contentLink: String? = "null"
    private lateinit var pageLayout: LinearLayout
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var animeModel: AnimeDetails
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var animeNameTxt: TextView
    private lateinit var animeDetailsTxt: TextView
    private lateinit var lastWatchedText: TextView
    private lateinit var coverImage: ImageView
    private lateinit var backgroundImage: ImageView
    private lateinit var lastWatchedPrefString: String
    private lateinit var buttonFavorite: ImageButton
    private lateinit var spinner: Spinner
    private lateinit var playAnimeButton: ImageButton

    private val args: AnimeFragmentArgs by navArgs()
    private val animeDetailsViewModel: AnimeDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeBinding.inflate(inflater, container, false)

        animeNameTxt = binding.animeNameTxt
        animeDetailsTxt = binding.animeDetailsTxt
        coverImage = binding.coverAnime
        backgroundImage = binding.backgroundImage
        progressBar = binding.progressbarInPage
        lastWatchedText = binding.lastWatchedTxt
        pageLayout = binding.pageLayout
        buttonFavorite = binding.buttonFavorite
        spinner = binding.episodeSpinner
        playAnimeButton = binding.episodeButtonForSpinner

        animeDetailsTxt.movementMethod = ScrollingMovementMethod()

        contentLink = args.animeLink

        if (contentLink == "null") {
            findNavController().popBackStack()
            Toast.makeText(activity, "Some Unexpected error occurred", Toast.LENGTH_SHORT).show()
        }

        sharedPreferences =
            activity!!.getSharedPreferences("LastWatchedPref", AppCompatActivity.MODE_PRIVATE)
        lastWatchedPrefString =
            sharedPreferences.getString(contentLink, "Not Started Yet").toString()

        // Check Favorite
        handleFavorite()

        pageLayout.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        animeDetailsViewModel.animeDetails.observe(viewLifecycleOwner) {
            animeModel = it
            animeNameTxt.text = animeModel.animeName
            animeDetailsTxt.text = animeModel.animeDesc

            lastWatchedText.text =
                if (lastWatchedPrefString == "Not Started Yet") lastWatchedPrefString
                else "Last Watched : $lastWatchedPrefString/${animeModel.animeEpisodes}"

            // load background image.
            backgroundImage.load(animeModel.animeCover) {
                error(R.drawable.ic_broken_image)
            }
            // load cover image.
            coverImage.load(animeModel.animeCover) {
                error(R.drawable.ic_broken_image)
            }
            progressBar.visibility = View.GONE
            pageLayout.visibility = View.VISIBLE
            setupSpinner(animeModel.animeEpisodes, animeModel.animeName,animeModel.animeEpisodes)
        }

        CoroutineScope(Dispatchers.IO).launch {
            contentLink?.let { animeDetailsViewModel.getAnimeDetails(it) }
        }

        return binding.root
    }

    private fun setupSpinner(num: String, animeName: String, animeEpisodes: String) {

        val epList = (num.toInt() downTo 1).map { it.toString() }
        val arrayAdapter =
            ArrayAdapter(activity as Context, android.R.layout.simple_spinner_item, epList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        // Remember Last watched in Spinner
        if (lastWatchedPrefString in epList)
            spinner.setSelection(epList.indexOf(lastWatchedPrefString))

        playAnimeButton.setOnClickListener {

            // Store Last Watched Episode
            sharedPreferences.edit()
                .putString(contentLink, spinner.selectedItem.toString()).apply()

            // Update to new value
            sharedPreferences = activity!!.getSharedPreferences(
                "LastWatchedPref",
                AppCompatActivity.MODE_PRIVATE
            )
            sharedPreferences.getString(contentLink, "Not Started Yet").apply {
                lastWatchedText.text =
                    if (this == "Not Started Yet") this else "Last Watched : $this/$animeEpisodes"
            }

            // Get the link of episode
            var watchLink = contentLink
            watchLink = watchLink?.replace("anime", "watch")
            val animeEpUrl = "https://yugen.to${watchLink}${spinner.selectedItem}"
            println(animeEpUrl)

            Intent(activity, PlayerActivity::class.java).apply {
                putExtra("anime_name", animeName)
                putExtra("anime_episode", "Episode ${spinner.selectedItem}")
                putExtra("anime_url", animeEpUrl)
                startActivity(this)
            }

        }


    }

    private fun handleFavorite() {
        // open DB
        var db = Room.databaseBuilder(
            activity as Context,
            LinksRoomDatabase::class.java, "fav-db"
        ).build()
        var linkDao = db.linkDao()

        CoroutineScope(Dispatchers.IO).launch {
            // get fav list
            val favList = linkDao.getLinks()
            db.close()
            withContext(Dispatchers.Main) {
                // check Fav
                val isFav = favList.any { it.linkString == contentLink }
                if (isFav)
                    inFav(buttonFavorite)
                else
                    notInFav(buttonFavorite)
            }
        }

        /*
        btn on click ->
            open db
            check is fav
            if fav ->
                remove from fav
                set icon
            not fav
                add to fav
                set icon
            close db
        */

        buttonFavorite.setOnClickListener {
            // open DB
            db = Room.databaseBuilder(
                activity as Context,
                LinksRoomDatabase::class.java, "fav-db"
            ).build()
            linkDao = db.linkDao()

            CoroutineScope(Dispatchers.IO).launch {
                // get fav list
                val favList = linkDao.getLinks()
                withContext(Dispatchers.Main) {
                    // check Fav
                    var isFav = false
                    var foundFav = FavRoomModel("null", "null", "null")
                    for (fav in favList) {
                        if (fav.linkString == contentLink) {
                            isFav = true
                            foundFav = fav
                            break
                        }
                    }

                    if (isFav) {
                        inFav(buttonFavorite)
                        CoroutineScope(Dispatchers.IO).launch {
                            linkDao.deleteOne(foundFav)
                            withContext(Dispatchers.Main) {
                                notInFav(buttonFavorite)
                            }
                        }.start()

                    } else {
                        notInFav(buttonFavorite)
                        CoroutineScope(Dispatchers.IO).launch {
                            linkDao.insert(
                                FavRoomModel(
                                    contentLink.toString(),
                                    animeModel.animeCover,
                                    animeModel.animeName
                                )
                            )
                            withContext(Dispatchers.Main) {
                                inFav(buttonFavorite)
                            }
                        }.start()
                    }

                    // end of main thread
                }

                // end of io thread
            }
            db.close()
        }
    }

    private fun inFav(buttonFavorite: ImageButton) {
        println("In Fav")
        buttonFavorite.setImageResource(R.drawable.ic_heart_minus)
    }

    private fun notInFav(buttonFavorite: ImageButton) {
        println("Not in Fav")
        buttonFavorite.setImageResource(R.drawable.ic_heart_plus)

    }

}