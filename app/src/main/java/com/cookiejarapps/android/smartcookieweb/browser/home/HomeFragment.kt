package com.cookiejarapps.android.smartcookieweb.browser.home

import android.util.Log
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Display.FLAG_SECURE
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode
import com.cookiejarapps.android.smartcookieweb.browser.ShortcutDatabase
import com.cookiejarapps.android.smartcookieweb.browser.UserPreferences
import com.cookiejarapps.android.smartcookieweb.browser.data.BrowserState
import com.cookiejarapps.android.smartcookieweb.browser.HomeFragmentDirections
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentHomeBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.view.MenuButton
import mozilla.components.ui.tabcounter.TabCounter
import java.lang.ref.WeakReference

class HomeFragment : Fragment() {
    private var database: ShortcutDatabase? = null

    private val args by navArgs<HomeFragmentArgs>()
    private lateinit var bundleArgs: Bundle

    private val browsingModeManager get() = (activity as BrowserActivity).browsingModeManager

    private val store: BrowserStore
        get() = components.store

    private var appBarLayout: AppBarLayout? = null

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @VisibleForTesting
    internal var getMenuButton: () -> MenuButton? = { binding.menuButton }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bundleArgs = args.toBundle()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        activity as BrowserActivity
        val components = requireContext().components

        updateLayout(view)

        if (!UserPreferences(requireContext()).showShortcuts) {
            binding.shortcutName.visibility = View.GONE
            binding.shortcutGrid.visibility = View.GONE
        }

        if (!UserPreferences(requireContext()).shortcutDrawerOpen) {
            binding.shortcutName.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_baseline_shortcuts, 0,
                R.drawable.ic_baseline_chevron_up, 0
            )
        }

        binding.shortcutToggleButton.setOnClickListener {
            val prefs = UserPreferences(requireContext())
            prefs.shortcutDrawerOpen = !prefs.shortcutDrawerOpen
            updateLayout(view)
        }

        binding.searchEngineIcon.setImageDrawable(null)

        // Övrig kod i onCreateView…
        return view
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeSearchEngineChanges()
        createHomeMenu(requireContext(), WeakReference(binding.menuButton))

        binding.gestureLayout.addGestureListener(
            onClick = { }, onScroll = { }, onSwipeDown = { },
            onSwipeUp = { }, onSwipeRight = { }, onSwipeLeft = { }
        )

        binding.powerOffFab.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Stäng appen?")
                .setMessage("Appen kommer att stängas helt.")
                .setPositiveButton("OK") { _, _ ->
                    requireActivity().finishAffinity()
                }
                .setNegativeButton("Avbryt", null)
                .show()
        }

        // Här kopplar vi btn_map till den nya MapsActivity
        binding.btnMap.setOnClickListener {
            val intent = Intent(requireContext(), MapsActivity::class.java)
            startActivity(intent)
        }

        binding.menuButton.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                R.color.main_icon
            )
        )

        binding.toolbarWrapper.setOnClickListener {
            navigateToSearch()
        }

        binding.tabButton.setOnClickListener {
            openTabDrawer()
        }

        /* Resten av observeFrom/store‐logiken, osv... */
        if (browsingModeManager.mode.isPrivate) {
            requireActivity().window.addFlags(FLAG_SECURE)
        } else {
            requireActivity().window.clearFlags(FLAG_SECURE)
        }

        consumeFrom(components.store) {
            updateTabCounter(it)
        }

        updateTabCounter(components.store.state)

        if (bundleArgs.getBoolean(FOCUS_ON_ADDRESS_BAR)) {
            navigateToSearch()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        appBarLayout = null
        bundleArgs.clear()
        requireActivity().window.clearFlags(FLAG_SECURE)
    }

    private fun navigateToSearch() {
        val directions =
            HomeFragmentDirections.actionGlobalSearchDialog(
                sessionId = null
            )

        nav(R.id.homeFragment, directions, null)
    }

    private fun createHomeMenu(context: Context, menuButtonRef: WeakReference<MenuButton>) {
        // Existerande kod för att skapa hem‐meny…
    }

    private fun observeSearchEngineChanges() {
        consumeFlow(store) { flow ->
            flow.map { state -> state.search.selectedOrDefaultSearchEngine }
                .distinctUntilChanged()
                .collect { searchEngine ->
                    if (searchEngine != null) {
                        val iconSize =
                            requireContext().resources.getDimensionPixelSize(R.dimen.icon_width)
                        val searchIcon =
                            BitmapDrawable(requireContext().resources, searchEngine.icon)
                        searchIcon.setBounds(0, 0, iconSize, iconSize)
                        binding.searchEngineIcon.setImageDrawable(searchIcon)
                    } else {
                        binding.searchEngineIcon.setImageDrawable(null)
                    }
                }
        }
    }

    private fun updateLayout(view: View) {
        // Existerande kod för att hantera layoutuppdatering…
    }

    private fun openTabDrawer() {
        // Existerande kod för att öppna flik‐dragspelaren…
    }

    private fun updateTabCounter(browserState: BrowserState) {
        val tabCount = if (browsingModeManager.mode.isPrivate) {
            browserState.privateTabs.size
        } else {
            browserState.normalTabs.size
        }

        binding.tabButton.setCountWithAnimation(tabCount)
    }

    companion object {
        private const val FOCUS_ON_ADDRESS_BAR = "focusOnAddressBar"
    }
}
