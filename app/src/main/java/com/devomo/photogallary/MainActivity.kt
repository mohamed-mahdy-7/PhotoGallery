package com.devomo.photogallary

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.devomo.data.Constants.Constans
import com.devomo.photogallary.adapter.PhotoAdapter
import com.devomo.photogallary.databinding.ActivityMainBinding
import com.devomo.photogallary.viewmodel.PhotoListState
import com.devomo.photogallary.viewmodel.PhotoListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binging: ActivityMainBinding
    private val viewModel: PhotoListViewModel by viewModels()
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var layoutManager: StaggeredGridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binging = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binging.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences(Constans.PREF_KEY, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        updateUi(sharedPreferences)

        setupRecyclerView()
        observeViewModel()
        observeNetworkStatus()


        binging.switchButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                editor.putBoolean(Constans.SWITCH_BUTTON_KEY, true).apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) // Apply immediately
            } else {
                editor.putBoolean(Constans.SWITCH_BUTTON_KEY, false).apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Apply immediately
            }

        }
    }

    private fun observeNetworkStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isOnline.collect { isOnline ->
                    if (isOnline) {
                        binging.networkStatusBanner.visibility = View.GONE
                    } else {
                        binging.networkStatusBanner.visibility = View.VISIBLE
                        binging.networkStatusBanner.text = "Offline Mode"
                        binging.networkStatusBanner.setBackgroundColor(getColor(android.R.color.darker_gray))
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.photoListState.collect { state ->
                    binging.progressBar.isVisible = state is PhotoListState.Loading
                    binging.textViewError.isVisible = state is PhotoListState.Error

                    when (state) {
                        is PhotoListState.Loading -> {

                        }

                        is PhotoListState.Success -> {
                            photoAdapter.submitList(state.photos)
                        }

                        is PhotoListState.Error -> {
                            binging.textViewError.text = state.message
                            Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        photoAdapter = PhotoAdapter()

        layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        binging.photoRv.apply {
            layoutManager = this@MainActivity.layoutManager
            adapter = photoAdapter
            addOnScrollListener(createPaginationScrollListener())
        }
    }

    private fun createPaginationScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null)
                    val lastVisibleItem = lastVisibleItemPositions.maxOrNull() ?: 0

                    val visibleThreshold = 5

                    if (!viewModel.isLoadingMore && !viewModel.isLastPage) {
                        if (lastVisibleItem >= totalItemCount - visibleThreshold) {
                            viewModel.loadNextPage()
                        }
                    }
                }
            }
        }
    }

    private fun updateUi(sharedPreferences: SharedPreferences?) {
        binging.switchButton.apply {
            val isDarkThemeEnabled =
                sharedPreferences?.getBoolean(Constans.SWITCH_BUTTON_KEY, false) ?: false
            isChecked = isDarkThemeEnabled

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
}