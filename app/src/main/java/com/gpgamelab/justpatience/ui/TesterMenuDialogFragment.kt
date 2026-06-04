package com.gpgamelab.justpatience.ui
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.material.button.MaterialButton
import com.gpgamelab.justpatience.BuildConfig
import com.gpgamelab.justpatience.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
/**
 * Tester-only popup menu for adjusting game state (gems, coupons, premium, full reset).
 */
class TesterMenuDialogFragment : DialogFragment() {
    interface Host {
        fun testerCurrentGems(): Int
        fun testerCurrentCoupons(): Int
        fun testerCurrentMagicWands(): Int
        fun testerIsPremium(): Boolean
        fun onTesterAdjustGems(delta: Int)
        fun onTesterSetGems(value: Int)
        fun onTesterAdjustCoupons(delta: Int)
        fun onTesterSetCoupons(value: Int)
        fun onTesterAdjustMagicWands(delta: Int)
        fun onTesterSetMagicWands(value: Int)
        fun onTesterSetPremium(enabled: Boolean)
        fun onTesterResetEverything(onComplete: () -> Unit)
        fun onTesterTriggerWinSequence()
        fun onTesterTriggerDailyBonus()
        fun onTesterTriggerNoTicketsPopup()
        fun testerDeviceAspectCategoryLabel(): String
    }

    private var tvPremiumStatus: TextView? = null
    private var btnPremiumToggle: MaterialButton? = null
    private var btnGemsValue: MaterialButton? = null
    private var btnCouponsValue: MaterialButton? = null
    private var btnMagicWandsValue: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_tester_menu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val host = activity as? Host ?: return
        tvPremiumStatus  = view.findViewById(R.id.tv_tester_premium_status)
        btnPremiumToggle = view.findViewById(R.id.btn_tester_premium_toggle)
        btnGemsValue     = view.findViewById(R.id.btn_gems_value)
        btnCouponsValue  = view.findViewById(R.id.btn_coupons_value)
        btnMagicWandsValue = view.findViewById(R.id.btn_magic_wands_value)
        refreshAllDisplays(host)

        // Gems
        view.findViewById<MaterialButton>(R.id.btn_gems_minus5).setOnClickListener { host.onTesterAdjustGems(-5); refreshGemsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_gems_minus1).setOnClickListener { host.onTesterAdjustGems(-1); refreshGemsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_gems_plus1).setOnClickListener  { host.onTesterAdjustGems(1);  refreshGemsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_gems_plus5).setOnClickListener  { host.onTesterAdjustGems(5);  refreshGemsDisplay(host) }
        btnGemsValue?.setOnClickListener {
            showSetValueDialog(getString(R.string.tester_menu_gems_section), host.testerCurrentGems()) { v ->
                host.onTesterSetGems(v); refreshGemsDisplay(host)
            }
        }
        // Coupons
        view.findViewById<MaterialButton>(R.id.btn_coupons_minus5).setOnClickListener { host.onTesterAdjustCoupons(-5); refreshCouponsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_coupons_minus1).setOnClickListener { host.onTesterAdjustCoupons(-1); refreshCouponsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_coupons_plus1).setOnClickListener  { host.onTesterAdjustCoupons(1);  refreshCouponsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_coupons_plus5).setOnClickListener  { host.onTesterAdjustCoupons(5);  refreshCouponsDisplay(host) }
        btnCouponsValue?.setOnClickListener {
            showSetValueDialog(getString(R.string.tester_menu_coupons_section), host.testerCurrentCoupons()) { v ->
                host.onTesterSetCoupons(v); refreshCouponsDisplay(host)
            }
        }

        // Magic Wands
        view.findViewById<MaterialButton>(R.id.btn_magic_wands_minus5).setOnClickListener { host.onTesterAdjustMagicWands(-5); refreshMagicWandsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_magic_wands_minus1).setOnClickListener { host.onTesterAdjustMagicWands(-1); refreshMagicWandsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_magic_wands_plus1).setOnClickListener  { host.onTesterAdjustMagicWands(1);  refreshMagicWandsDisplay(host) }
        view.findViewById<MaterialButton>(R.id.btn_magic_wands_plus5).setOnClickListener  { host.onTesterAdjustMagicWands(5);  refreshMagicWandsDisplay(host) }
        btnMagicWandsValue?.setOnClickListener {
            showSetValueDialog(getString(R.string.tester_menu_magic_wands_section), host.testerCurrentMagicWands()) { v ->
                host.onTesterSetMagicWands(v); refreshMagicWandsDisplay(host)
            }
        }

        // Premium toggle
        btnPremiumToggle?.setOnClickListener {
            host.onTesterSetPremium(!host.testerIsPremium())
            refreshPremiumDisplay(host)
        }
        // Reset Everything
        view.findViewById<MaterialButton>(R.id.btn_tester_reset).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.tester_menu_reset_confirm_title)
                .setMessage(R.string.tester_menu_reset_confirm_message)
                .setPositiveButton(R.string.tester_menu_reset_confirm_yes) { _, _ ->
                    host.onTesterResetEverything { refreshAllDisplays(host) }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        // Device Info (replaces standalone AdMob Device ID button)
        view.findViewById<MaterialButton>(R.id.btn_tester_admob_device_id).setOnClickListener {
            fetchAndShowDeviceInfo(host)
        }

        // Close
        view.findViewById<MaterialButton>(R.id.btn_tester_close).setOnClickListener { dismiss() }
         // Trigger Win Sequence
         view.findViewById<MaterialButton>(R.id.btn_tester_trigger_win).setOnClickListener {
             dismiss()
             host.onTesterTriggerWinSequence()
         }
         // Trigger Daily Bonus
         view.findViewById<MaterialButton>(R.id.btn_tester_trigger_daily_bonus).setOnClickListener {
             dismiss()
             host.onTesterTriggerDailyBonus()
         }
         // Trigger No Tickets Popup
         view.findViewById<MaterialButton>(R.id.btn_tester_trigger_no_tickets).setOnClickListener {
             dismiss()
             host.onTesterTriggerNoTicketsPopup()
         }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { win ->
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val widthFraction = if (isLandscape) 0.60f else 0.92f
            val widthPx = (resources.displayMetrics.widthPixels * widthFraction).toInt()
            win.setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
    private fun refreshAllDisplays(host: Host) {
        refreshGemsDisplay(host)
        refreshCouponsDisplay(host)
        refreshMagicWandsDisplay(host)
        refreshPremiumDisplay(host)
    }

    private fun refreshGemsDisplay(host: Host) {
        btnGemsValue?.text = host.testerCurrentGems().toString()
    }

    private fun refreshCouponsDisplay(host: Host) {
        btnCouponsValue?.text = host.testerCurrentCoupons().toString()
    }

    private fun refreshMagicWandsDisplay(host: Host) {
        btnMagicWandsValue?.text = host.testerCurrentMagicWands().toString()
    }

    private fun refreshPremiumDisplay(host: Host) {
        val isPremium = host.testerIsPremium()
        tvPremiumStatus?.text = if (isPremium) getString(R.string.tester_menu_premium_enabled)
                                else getString(R.string.tester_menu_premium_disabled)
        btnPremiumToggle?.text = if (isPremium) getString(R.string.tester_menu_disable)
                                 else getString(R.string.tester_menu_enable)
    }

    private fun showSetValueDialog(label: String, current: Int, onValueSet: (Int) -> Unit) {
        val editText = EditText(requireContext()).apply {
            hint      = getString(R.string.tester_menu_set_value_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
            setPadding(40, 24, 40, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.tester_menu_set_value_title, label))
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val raw = editText.text.toString().trim().toIntOrNull() ?: current
                onValueSet(raw.coerceAtLeast(0))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun fetchAndShowDeviceInfo(host: Host) {
        val ctx = requireContext().applicationContext
        val dm = resources.displayMetrics
        val widthPx = dm.widthPixels
        val heightPx = dm.heightPixels
        val widthDp = (widthPx / dm.density).toInt()
        val heightDp = (heightPx / dm.density).toInt()
        val density = dm.density
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val softwareChannel = BuildConfig.BUILD_TYPE
        val deviceProfile = host.testerDeviceAspectCategoryLabel()
        val tz = TimeZone.getDefault()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = tz }
        val dateTime = sdf.format(Date())
        val tzDisplay = "${tz.id} (${tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT)})"
        val languages = ctx.resources.configuration.locales.let { list ->
            (0 until list.size()).joinToString(", ") { list.get(it).toLanguageTag() }
        }
        val ramMb = try {
            val actMgr = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            actMgr.getMemoryInfo(memInfo)
            val totalRamMb = memInfo.totalMem / (1024 * 1024)
            "${totalRamMb} MB"
        } catch (e: Exception) { "unavailable" }

        CoroutineScope(Dispatchers.IO).launch {
            val idInfo = try { AdvertisingIdClient.getAdvertisingIdInfo(ctx) } catch (e: Exception) { null }
            val rawId = idInfo?.id ?: "unavailable"
            val admobHash = try {
                val md = java.security.MessageDigest.getInstance("MD5")
                md.update(rawId.toByteArray())
                md.digest().joinToString("") { "%02X".format(it) }
            } catch (e: Exception) { rawId }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                val message = buildString {
                    appendLine("Device Model:  $deviceModel")
                    appendLine("Android:       $androidVersion")
                    appendLine("Resolution:    ${widthPx}×${heightPx} px  /  ${widthDp}×${heightDp} dp  (${String.format("%.2f", density)}x)")
                    appendLine("RAM:           $ramMb")
                    appendLine("Device Profile: $deviceProfile")
                    appendLine("Channel:       $softwareChannel")
                    appendLine("Date/Time:     $dateTime")
                    appendLine("Time Zone:     $tzDisplay")
                    appendLine("Languages:     $languages")
                    appendLine()
                    appendLine("─── AdMob ───")
                    appendLine("Advertising ID:  $rawId")
                    append("AdMob Hash:      $admobHash")
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Device Info")
                    .setMessage(message)
                    .setPositiveButton("Copy AdMob Hash") { _, _ ->
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("admob_device_id", admobHash))
                        Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
                    }
                    .setNeutralButton("Copy All") { _, _ ->
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("device_info", message))
                        Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    companion object {
        const val TAG = "tester_menu_dialog"
        fun newInstance(): TesterMenuDialogFragment = TesterMenuDialogFragment()
    }
}
