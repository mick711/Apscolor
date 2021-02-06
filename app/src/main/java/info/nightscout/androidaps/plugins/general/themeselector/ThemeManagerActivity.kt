package info.nightscout.androidaps.plugins.general.themeselector

import android.R.attr
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.preference.ColorPickerPreferenceManager
import info.nightscout.androidaps.MainActivity
import info.nightscout.androidaps.R
import info.nightscout.androidaps.databinding.ThemeselectorBottomSheetBinding
import info.nightscout.androidaps.databinding.ThemeselectorScrollingFragmentBinding
import info.nightscout.androidaps.plugins.general.colorpicker.CustomFlag
import info.nightscout.androidaps.plugins.general.themeselector.adapter.RecyclerViewClickListener
import info.nightscout.androidaps.plugins.general.themeselector.adapter.ThemeAdapter
import info.nightscout.androidaps.plugins.general.themeselector.model.Theme
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil.THEME_DEFAULT
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil.getThemeId
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil.themeList
import info.nightscout.androidaps.plugins.general.themeselector.view.ThemeView
import kotlinx.android.synthetic.main.themeselector_bottom_sheet.view.*
import java.util.*

class ThemeManagerActivity : MainActivity(), View.OnClickListener {

    private lateinit var binding: ThemeselectorScrollingFragmentBinding

    companion object {
        var mThemeList: MutableList<Theme> = ArrayList()
        var selectedTheme = 0

        init {
            selectedTheme = 0
        }
    }

    private var actualTheme = 0
    private var mAdapter: ThemeAdapter? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ThemeselectorScrollingFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBottomSheet()
        prepareThemeData()
        actualTheme = spSplash.getInt("theme", THEME_DEFAULT)
        val themeView = findViewById<ThemeView>(R.id.theme_selected)
        themeView.setTheme(mThemeList[actualTheme], actualTheme)
        setBackground()
    }

    private fun setBackground() {
        // get theme attribute
        val a = TypedValue()
        val drawable: Drawable
        theme.resolveAttribute(attr.windowBackground, a, true)
        if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            // windowBackground is a color
            drawable =  ColorDrawable(a.data)
        } else {
            // windowBackground is not a color, probably a drawable
            drawable = resources.getDrawable(a.resourceId, theme)
        }


        if ( spSplash.getBoolean("daynight", true)) {
            val cd = ColorDrawable(spSplash.getInt("darkBackgroundColor", info.nightscout.androidaps.core.R.color.background_dark))
            if ( !spSplash.getBoolean("backgroundcolor", true)) {
                binding.scrollingactivity.background =  cd
            } else {
                binding.scrollingactivity.background =  drawable
            }
        } else {
            val cd = ColorDrawable(spSplash.getInt("lightBackgroundColor", info.nightscout.androidaps.core.R.color.background_light))
            if ( !spSplash.getBoolean("backgroundcolor", true)) {
                binding.scrollingactivity.background =  cd
            } else {
                binding.scrollingactivity.background =  drawable
            }
        }
    }


    private fun initBottomSheet() {
        val nightMode = spSplash.getBoolean("daynight", true)
        // init the bottom sheet behavior
        mBottomSheetBehavior = BottomSheetBehavior.from( binding.scrollingactivity.bottom_sheet)
        val backGround = spSplash.getBoolean("backgroundcolor", true)
        binding.scrollingactivity.bottom_sheet.switch_backgroundimage.isChecked = backGround
        binding.scrollingactivity.bottom_sheet.switch_backgroundimage.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            spSplash.putBoolean("backgroundcolor", b)
            val delayTime = 200
            compoundButton.postDelayed(Runnable { changeTheme(spSplash.getInt("theme", THEME_DEFAULT)) }, delayTime.toLong())
        }
        binding.scrollingactivity.bottom_sheet.switch_dark_mode.isChecked = nightMode
        binding.scrollingactivity.bottom_sheet.switch_dark_mode.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            spSplash.putBoolean("daynight", b)
            var delayTime = 200
            if ((mBottomSheetBehavior as BottomSheetBehavior<*>).getState() == BottomSheetBehavior.STATE_EXPANDED) {
                delayTime = 400
                (mBottomSheetBehavior as BottomSheetBehavior<*>).setState(BottomSheetBehavior.STATE_EXPANDED)
            }
            compoundButton.postDelayed(object : Runnable {
                override fun run() {
                    if (b) {
                        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
                    }
                    changeTheme(spSplash.getInt("theme", THEME_DEFAULT))
                }
            }, delayTime.toLong())
        }
        binding.scrollingactivity.bottom_sheet.select_backgroundcolordark.setBackgroundColor(spSplash.getInt("darkBackgroundColor", ContextCompat.getColor(this, info.nightscout.androidaps.core.R.color.background_dark)))
        binding.scrollingactivity.bottom_sheet.select_backgroundcolorlight.setBackgroundColor(spSplash.getInt("lightBackgroundColor", ContextCompat.getColor(this, info.nightscout.androidaps.core.R.color.background_light)))
        binding.scrollingactivity.bottom_sheet.select_backgroundcolordark.setOnClickListener(View.OnClickListener { selectColor("dark") })
        binding.scrollingactivity.bottom_sheet.select_backgroundcolorlight.setOnClickListener(View.OnClickListener { selectColor("light") })

        binding.scrollingactivity.bottom_sheet.setDefaultColorDark?.setOnClickListener(View.OnClickListener {
            spSplash.putInt("darkBackgroundColor", ContextCompat.getColor(this, R.color.background_dark))
            binding.scrollingactivity.bottom_sheet.select_backgroundcolordark!!.setBackgroundColor(getColor((R.color.background_dark)))
            val delayTime = 200
            binding.scrollingactivity.bottom_sheet.select_backgroundcolordark!!.postDelayed({ changeTheme(spSplash.getInt("theme", THEME_DEFAULT)) }, delayTime.toLong())
        })

        binding.scrollingactivity.bottom_sheet.setDefaultColorLight?.setOnClickListener(View.OnClickListener {
            spSplash.putInt("lightBackgroundColor", ContextCompat.getColor(this, R.color.background_light))
            binding.scrollingactivity.bottom_sheet.select_backgroundcolorlight!!.setBackgroundColor(getColor((R.color.background_light)))
            val delayTime = 200
            binding.scrollingactivity.bottom_sheet.select_backgroundcolorlight!!.postDelayed({ changeTheme(spSplash.getInt("theme", THEME_DEFAULT)) }, delayTime.toLong())
        })

        mAdapter = ThemeAdapter(spSplash, mThemeList, object : RecyclerViewClickListener {
            override fun onClick(view: View?, position: Int) {
                (mBottomSheetBehavior as BottomSheetBehavior<*>).setState(BottomSheetBehavior.STATE_EXPANDED)
                view!!.postDelayed({
                    val themeView = findViewById<ThemeView>(R.id.theme_selected)
                    themeView.setTheme(mThemeList[selectedTheme], getThemeId(selectedTheme))
                    changeTheme(selectedTheme)
                }, 500)
            }
        })
        val mLayoutManager: RecyclerView.LayoutManager = GridLayoutManager(applicationContext, 3)
        binding.scrollingactivity.bottom_sheet.recyclerView.setLayoutManager(mLayoutManager)
        binding.scrollingactivity.bottom_sheet.recyclerView.setItemAnimator(DefaultItemAnimator())
        binding.scrollingactivity.bottom_sheet.recyclerView.setAdapter(mAdapter)
    }

    private fun prepareThemeData() {
        mThemeList.clear()
        mThemeList.addAll(themeList)
        mAdapter!!.notifyDataSetChanged()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.fab -> when (mBottomSheetBehavior!!.state) {
                BottomSheetBehavior.STATE_HIDDEN    -> mBottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
                BottomSheetBehavior.STATE_COLLAPSED -> mBottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
                BottomSheetBehavior.STATE_EXPANDED  -> mBottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }
    }

    private fun selectColor(lightOrDark: String) {
        val colorPickerDialog = ColorPickerDialog.Builder(this)
            .setTitle("Select Background Color")
            .setPreferenceName("MyColorPickerDialog")
            .setPositiveButton(getString(R.string.confirm),
                ColorEnvelopeListener { envelope, _ -> //setLayoutColor(envelope);
                    if (lightOrDark === "light") {
                        spSplash.putInt("lightBackgroundColor", envelope.color)
                        binding.scrollingactivity.bottom_sheet.select_backgroundcolorlight!!.setBackgroundColor(envelope.color)
                        val delayTime = 200
                        binding.scrollingactivity.bottom_sheet.select_backgroundcolorlight!!.postDelayed({ changeTheme(spSplash.getInt("theme", THEME_DEFAULT)) }, delayTime.toLong())
                    } else if (lightOrDark === "dark") {
                        spSplash.putInt("darkBackgroundColor", envelope.color)
                        binding.scrollingactivity.bottom_sheet.select_backgroundcolordark!!.setBackgroundColor(envelope.color)
                        val delayTime = 200
                        binding.scrollingactivity.bottom_sheet.select_backgroundcolordark!!.postDelayed({ changeTheme(spSplash.getInt("theme", THEME_DEFAULT)) }, delayTime.toLong())
                    }
                })
            .setNegativeButton(getString(R.string.cancel)
            ) { dialogInterface, _ -> dialogInterface.dismiss() }
            .attachAlphaSlideBar(false) // default is true. If false, do not show the AlphaSlideBar.
            .attachBrightnessSlideBar(true) // default is true. If false, do not show the BrightnessSlideBar.
            .setBottomSpace(12) // set bottom space between the last slidebar and buttons.

        val colorPickerView: ColorPickerView = colorPickerDialog.colorPickerView
        colorPickerView.setFlagView(CustomFlag(this, R.layout.colorpicker_flagview)) // sets a custom flagView
        colorPickerView.preferenceName = "AAPS-ColorPicker"
        colorPickerView.setLifecycleOwner(this)

        val manager = ColorPickerPreferenceManager.getInstance(this)
        if (lightOrDark === "light") {
            manager.setColor("AAPS-ColorPicker", spSplash.getInt("lightBackgroundColor", ContextCompat.getColor(this, info.nightscout.androidaps.core.R.color.background_light)))
        } else if (lightOrDark === "dark") {
            manager.setColor("AAPS-ColorPicker", spSplash.getInt("darkBackgroundColor", ContextCompat.getColor(this, info.nightscout.androidaps.core.R.color.background_dark)))
        }
        var point = Point(0,0)
        manager.getSelectorPosition("AAPS-ColorPicker",point)
        if(point === Point(0,0)) manager.setSelectorPosition("AAPS-ColorPicker", Point(530, 520))
        colorPickerDialog.show()

        setBackground()
    }
}