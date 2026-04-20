package com.gpgamelab.justpatience.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.gpgamelab.justpatience.R

class DevelopMenuDialogFragment : DialogFragment() {

	interface Host {
		fun testerStarburstPivotOffsetX(): Int
		fun testerStarburstPivotOffsetY(): Int
		fun onTesterAdjustStarburstPivotOffsetX(delta: Int)
		fun onTesterAdjustStarburstPivotOffsetY(delta: Int)
		fun onTesterSetStarburstPivotOffsetX(value: Int)
		fun onTesterSetStarburstPivotOffsetY(value: Int)
		fun testerStarburstPositionX(): Int
		fun testerStarburstPositionY(): Int
		fun onTesterAdjustStarburstPositionX(delta: Int)
		fun onTesterAdjustStarburstPositionY(delta: Int)
		fun onTesterSetStarburstPositionX(value: Int)
		fun onTesterSetStarburstPositionY(value: Int)
		fun testerStarburstScale(): Float
		fun onTesterAdjustStarburstScale(delta: Float)
		fun onTesterSetStarburstScale(value: Float)
		fun testerStarburstRotationDurationMs(): Int
		fun onTesterAdjustStarburstRotationDurationMs(delta: Int)
		fun onTesterSetStarburstRotationDurationMs(value: Int)
		fun testerIsStarburstRotationEnabled(): Boolean
		fun onTesterSetStarburstRotationEnabled(enabled: Boolean)
		fun onTesterApplyAutoStarburstLayout()
	}

	private var btnStarburstPivotXValue: MaterialButton? = null
	private var btnStarburstPivotYValue: MaterialButton? = null
	private var btnStarburstPositionXValue: MaterialButton? = null
	private var btnStarburstPositionYValue: MaterialButton? = null
	private var btnStarburstScaleValue: MaterialButton? = null
	private var btnStarburstRotationSpeedValue: MaterialButton? = null
	private var tvStarburstRotationStatus: TextView? = null
	private var btnStarburstRotationToggle: MaterialButton? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		isCancelable = false
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View = inflater.inflate(R.layout.dialog_develop_menu, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val host = activity as? Host ?: return

		btnStarburstPivotXValue = view.findViewById(R.id.btn_starburst_pivot_x_value)
		btnStarburstPivotYValue = view.findViewById(R.id.btn_starburst_pivot_y_value)
		btnStarburstPositionXValue = view.findViewById(R.id.btn_starburst_position_x_value)
		btnStarburstPositionYValue = view.findViewById(R.id.btn_starburst_position_y_value)
		btnStarburstScaleValue = view.findViewById(R.id.btn_starburst_scale_value)
		btnStarburstRotationSpeedValue = view.findViewById(R.id.btn_starburst_rotation_speed_value)
		tvStarburstRotationStatus = view.findViewById(R.id.tv_starburst_rotation_status)
		btnStarburstRotationToggle = view.findViewById(R.id.btn_starburst_rotation_toggle)

		val starburstHeader = view.findViewById<View>(R.id.layout_develop_starburst_header)
		val starburstArrow = view.findViewById<TextView>(R.id.tv_develop_starburst_arrow)
		val starburstContent = view.findViewById<LinearLayout>(R.id.layout_develop_starburst_content)
		var starburstExpanded = true
		setSectionExpanded(starburstContent, starburstArrow, starburstExpanded)
		starburstHeader.setOnClickListener {
			starburstExpanded = !starburstExpanded
			setSectionExpanded(starburstContent, starburstArrow, starburstExpanded)
		}

		refreshAllDisplays(host)

		view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_x_minus5).setOnClickListener {
			host.onTesterAdjustStarburstPivotOffsetX(-5)
			refreshStarburstPivotDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_x_minus1).setOnClickListener {
			host.onTesterAdjustStarburstPivotOffsetX(-1)
			refreshStarburstPivotDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_x_plus1).setOnClickListener {
			host.onTesterAdjustStarburstPivotOffsetX(1)
			refreshStarburstPivotDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_x_plus5).setOnClickListener {
			host.onTesterAdjustStarburstPivotOffsetX(5)
			refreshStarburstPivotDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_y_minus5).setOnClickListener {
			host.onTesterAdjustStarburstPivotOffsetY(-5)
			refreshStarburstPivotDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_y_minus1).setOnClickListener {
			host.onTesterAdjustStarburstPivotOffsetY(-1)
			refreshStarburstPivotDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_y_plus1).setOnClickListener {
			host.onTesterAdjustStarburstPivotOffsetY(1)
			refreshStarburstPivotDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_y_plus5).setOnClickListener {
			host.onTesterAdjustStarburstPivotOffsetY(5)
			refreshStarburstPivotDisplay(host)
		}
		btnStarburstPivotXValue?.setOnClickListener {
			showSetSignedValueDialog(getString(R.string.tester_menu_starburst_pivot_x), host.testerStarburstPivotOffsetX()) { value ->
				host.onTesterSetStarburstPivotOffsetX(value)
				refreshStarburstPivotDisplay(host)
			}
		}
		btnStarburstPivotYValue?.setOnClickListener {
			showSetSignedValueDialog(getString(R.string.tester_menu_starburst_pivot_y), host.testerStarburstPivotOffsetY()) { value ->
				host.onTesterSetStarburstPivotOffsetY(value)
				refreshStarburstPivotDisplay(host)
			}
		}

		view.findViewById<MaterialButton>(R.id.btn_starburst_position_x_minus10).setOnClickListener {
			host.onTesterAdjustStarburstPositionX(-10)
			refreshStarburstPositionDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_position_x_minus1).setOnClickListener {
			host.onTesterAdjustStarburstPositionX(-1)
			refreshStarburstPositionDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_position_x_plus1).setOnClickListener {
			host.onTesterAdjustStarburstPositionX(1)
			refreshStarburstPositionDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_position_x_plus10).setOnClickListener {
			host.onTesterAdjustStarburstPositionX(10)
			refreshStarburstPositionDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_position_y_minus10).setOnClickListener {
			host.onTesterAdjustStarburstPositionY(-10)
			refreshStarburstPositionDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_position_y_minus1).setOnClickListener {
			host.onTesterAdjustStarburstPositionY(-1)
			refreshStarburstPositionDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_position_y_plus1).setOnClickListener {
			host.onTesterAdjustStarburstPositionY(1)
			refreshStarburstPositionDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_position_y_plus10).setOnClickListener {
			host.onTesterAdjustStarburstPositionY(10)
			refreshStarburstPositionDisplay(host)
		}
		btnStarburstPositionXValue?.setOnClickListener {
			showSetSignedValueDialog(getString(R.string.tester_menu_starburst_position_x), host.testerStarburstPositionX()) { value ->
				host.onTesterSetStarburstPositionX(value)
				refreshStarburstPositionDisplay(host)
			}
		}
		btnStarburstPositionYValue?.setOnClickListener {
			showSetSignedValueDialog(getString(R.string.tester_menu_starburst_position_y), host.testerStarburstPositionY()) { value ->
				host.onTesterSetStarburstPositionY(value)
				refreshStarburstPositionDisplay(host)
			}
		}

		view.findViewById<MaterialButton>(R.id.btn_starburst_scale_minus_quarter).setOnClickListener {
			host.onTesterAdjustStarburstScale(-0.25f)
			refreshStarburstScaleDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_scale_minus_small).setOnClickListener {
			host.onTesterAdjustStarburstScale(-0.05f)
			refreshStarburstScaleDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_scale_plus_small).setOnClickListener {
			host.onTesterAdjustStarburstScale(0.05f)
			refreshStarburstScaleDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_scale_plus_quarter).setOnClickListener {
			host.onTesterAdjustStarburstScale(0.25f)
			refreshStarburstScaleDisplay(host)
		}
		btnStarburstScaleValue?.setOnClickListener {
			showSetDecimalValueDialog(getString(R.string.tester_menu_starburst_scale), host.testerStarburstScale()) { value ->
				host.onTesterSetStarburstScale(value)
				refreshStarburstScaleDisplay(host)
			}
		}

		view.findViewById<MaterialButton>(R.id.btn_starburst_rotation_speed_minus100).setOnClickListener {
			host.onTesterAdjustStarburstRotationDurationMs(-100)
			refreshStarburstRotationSpeedDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_rotation_speed_minus10).setOnClickListener {
			host.onTesterAdjustStarburstRotationDurationMs(-10)
			refreshStarburstRotationSpeedDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_rotation_speed_plus10).setOnClickListener {
			host.onTesterAdjustStarburstRotationDurationMs(10)
			refreshStarburstRotationSpeedDisplay(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_rotation_speed_plus100).setOnClickListener {
			host.onTesterAdjustStarburstRotationDurationMs(100)
			refreshStarburstRotationSpeedDisplay(host)
		}
		btnStarburstRotationSpeedValue?.setOnClickListener {
			showSetValueDialog(getString(R.string.tester_menu_starburst_rotation_speed), host.testerStarburstRotationDurationMs()) { value ->
				host.onTesterSetStarburstRotationDurationMs(value)
				refreshStarburstRotationSpeedDisplay(host)
			}
		}

		btnStarburstRotationToggle?.setOnClickListener {
			host.onTesterSetStarburstRotationEnabled(!host.testerIsStarburstRotationEnabled())
			refreshStarburstRotationDisplay(host)
		}

		view.findViewById<MaterialButton>(R.id.btn_starburst_copy_values).setOnClickListener {
			copyStarburstValuesToClipboard(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_starburst_apply_auto).setOnClickListener {
			host.onTesterApplyAutoStarburstLayout()
			refreshAllDisplays(host)
		}
		view.findViewById<MaterialButton>(R.id.btn_develop_close).setOnClickListener { dismiss() }
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
		refreshStarburstPivotDisplay(host)
		refreshStarburstPositionDisplay(host)
		refreshStarburstScaleDisplay(host)
		refreshStarburstRotationSpeedDisplay(host)
		refreshStarburstRotationDisplay(host)
	}

	private fun refreshStarburstPivotDisplay(host: Host) {
		btnStarburstPivotXValue?.text = host.testerStarburstPivotOffsetX().toString()
		btnStarburstPivotYValue?.text = host.testerStarburstPivotOffsetY().toString()
	}

	private fun refreshStarburstPositionDisplay(host: Host) {
		btnStarburstPositionXValue?.text = host.testerStarburstPositionX().toString()
		btnStarburstPositionYValue?.text = host.testerStarburstPositionY().toString()
	}

	private fun refreshStarburstScaleDisplay(host: Host) {
		btnStarburstScaleValue?.text = String.format(java.util.Locale.US, "%.2fx", host.testerStarburstScale())
	}

	private fun refreshStarburstRotationSpeedDisplay(host: Host) {
		btnStarburstRotationSpeedValue?.text = host.testerStarburstRotationDurationMs().toString()
	}

	private fun refreshStarburstRotationDisplay(host: Host) {
		val enabled = host.testerIsStarburstRotationEnabled()
		tvStarburstRotationStatus?.text = if (enabled) {
			getString(R.string.tester_menu_starburst_rotation_running)
		} else {
			getString(R.string.tester_menu_starburst_rotation_stopped)
		}
		btnStarburstRotationToggle?.text = if (enabled) {
			getString(R.string.tester_menu_starburst_rotation_stop)
		} else {
			getString(R.string.tester_menu_starburst_rotation_start)
		}
	}

	private fun setSectionExpanded(content: View, arrow: TextView, expanded: Boolean) {
		content.visibility = if (expanded) View.VISIBLE else View.GONE
		arrow.text = if (expanded) "▴" else "▾"
	}

	private fun showSetValueDialog(label: String, current: Int, onValueSet: (Int) -> Unit) {
		val editText = EditText(requireContext()).apply {
			hint = getString(R.string.tester_menu_set_value_hint)
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

	private fun showSetSignedValueDialog(label: String, current: Int, onValueSet: (Int) -> Unit) {
		val editText = EditText(requireContext()).apply {
			hint = getString(R.string.tester_menu_set_value_hint)
			inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
			setText(current.toString())
			setSelection(text.length)
			setPadding(40, 24, 40, 24)
		}
		AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.tester_menu_set_value_title, label))
			.setView(editText)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val raw = editText.text.toString().trim().toIntOrNull() ?: current
				onValueSet(raw)
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	private fun showSetDecimalValueDialog(label: String, current: Float, onValueSet: (Float) -> Unit) {
		val editText = EditText(requireContext()).apply {
			hint = getString(R.string.tester_menu_set_value_hint)
			inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
			setText(String.format(java.util.Locale.US, "%.2f", current))
			setSelection(text.length)
			setPadding(40, 24, 40, 24)
		}
		AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.tester_menu_set_value_title, label))
			.setView(editText)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val raw = editText.text.toString().trim().toFloatOrNull() ?: current
				onValueSet(raw)
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	private fun copyStarburstValuesToClipboard(host: Host) {
		val values = buildString {
			appendLine("// Starburst tuning values")
			appendLine("positionX = ${host.testerStarburstPositionX()}")
			appendLine("positionY = ${host.testerStarburstPositionY()}")
			appendLine("scale = ${String.format(java.util.Locale.US, "%.2f", host.testerStarburstScale())}")
			appendLine("pivotOffsetX = ${host.testerStarburstPivotOffsetX()}")
			appendLine("pivotOffsetY = ${host.testerStarburstPivotOffsetY()}")
			appendLine("rotationDurationMs = ${host.testerStarburstRotationDurationMs()}")
			append("rotationEnabled = ${host.testerIsStarburstRotationEnabled()}")
		}
		val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		clipboard.setPrimaryClip(ClipData.newPlainText("Starburst Values", values))
		Toast.makeText(requireContext(), R.string.tester_menu_starburst_values_copied, Toast.LENGTH_SHORT).show()
	}

	companion object {
		const val TAG = "develop_menu_dialog"

		fun newInstance(): DevelopMenuDialogFragment = DevelopMenuDialogFragment()
	}
}

