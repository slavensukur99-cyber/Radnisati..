package com.radnisati.app

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {
    private val bg = Color.rgb(13, 17, 30)
    private val panel = Color.rgb(25, 31, 49)
    private val cyan = Color.rgb(53, 216, 208)
    private val purple = Color.rgb(124, 108, 255)
    private val white = Color.rgb(240, 244, 255)
    private val muted = Color.rgb(160, 170, 195)
    private val prefs by lazy { getSharedPreferences("radni_sati", MODE_PRIVATE) }
    private var month = YearMonth.now()
    private var selected = LocalDate.now()
    private var start = "08:00"
    private var end = "16:00"
    private var note = ""
    private lateinit var root: LinearLayout
    private lateinit var monthLabel: TextView
    private lateinit var calendar: GridLayout
    private lateinit var startButton: Button
    private lateinit var endButton: Button
    private lateinit var noteInput: EditText
    private lateinit var dayTotal: TextView
    private lateinit var nightTotal: TextView
    private lateinit var monthTotals: TextView
    private lateinit var selectedLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        buildUi()
        loadDay()
        renderCalendar()
        updateTotals()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(22))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        val heading = row()
        val logo = TextView(this).apply {
            text = "⌛"; textSize = 32f; gravity = Gravity.CENTER
            setTextColor(cyan); background = rounded(panel, 20)
        }
        heading.addView(logo, LinearLayout.LayoutParams(dp(58), dp(58)))
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        titleBox.addView(label("RadniSati", 25f, white, true))
        titleBox.addView(label("Tvoja evidencija, uvijek pri ruci", 12f, muted))
        heading.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(heading)
        content.addView(space(18))

        val monthRow = row()
        val prev = button("‹", purple).apply { setOnClickListener { month = month.minusMonths(1); selected = month.atDay(1); loadDay(); renderCalendar(); updateTotals() } }
        val next = button("›", purple).apply { setOnClickListener { month = month.plusMonths(1); selected = month.atDay(1); loadDay(); renderCalendar(); updateTotals() } }
        monthRow.addView(prev, LinearLayout.LayoutParams(dp(48), dp(46)))
        monthLabel = label("", 18f, white, true).apply { gravity = Gravity.CENTER }
        monthRow.addView(monthLabel, LinearLayout.LayoutParams(0, dp(46), 1f))
        monthRow.addView(next, LinearLayout.LayoutParams(dp(48), dp(46)))
        content.addView(monthRow)
        content.addView(space(12))

        calendar = GridLayout(this).apply { columnCount = 7; alignmentMode = GridLayout.ALIGN_BOUNDS }
        content.addView(calendar)
        content.addView(space(16))

        val summaryCard = card()
        summaryCard.addView(label("PREGLED MJESECA", 11f, muted, true))
        monthTotals = label("", 15f, white, true)
        summaryCard.addView(monthTotals)
        content.addView(summaryCard)
        content.addView(space(16))

        val editCard = card()
        selectedLabel = label("", 18f, white, true)
        editCard.addView(selectedLabel)
        editCard.addView(space(10))
        editCard.addView(label("Početak rada", 12f, muted))
        startButton = button(start, cyan).apply { setOnClickListener { chooseTime(true) } }
        editCard.addView(startButton, LinearLayout.LayoutParams(-1, dp(48)))
        editCard.addView(space(8))
        editCard.addView(label("Završetak rada", 12f, muted))
        endButton = button(end, purple).apply { setOnClickListener { chooseTime(false) } }
        editCard.addView(endButton, LinearLayout.LayoutParams(-1, dp(48)))
        editCard.addView(space(12))
        editCard.addView(label("Napomena za ovaj dan", 12f, muted))
        noteInput = EditText(this).apply {
            setTextColor(white); setHintTextColor(muted); hint = "Npr. smjena, prekovremeni rad..."
            textSize = 14f; minLines = 2; maxLines = 4; gravity = Gravity.TOP
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(17, 22, 37), 14)
        }
        editCard.addView(noteInput, LinearLayout.LayoutParams(-1, dp(94)))
        editCard.addView(space(12))
        val save = button("Sačuvaj evidenciju", cyan).apply {
            setOnClickListener {
                note = noteInput.text.toString()
                saveDay()
                renderCalendar()
                updateTotals()
                animateIn(this)
                Toast.makeText(this@MainActivity, "Evidencija je sačuvana na telefonu", Toast.LENGTH_SHORT).show()
            }
        }
        editCard.addView(save, LinearLayout.LayoutParams(-1, dp(50)))
        content.addView(editCard)
        content.addView(space(16))

        val hoursCard = card()
        hoursCard.addView(label("IZABRANI DAN", 11f, muted, true))
        dayTotal = label("Dnevni rad  ·  0 h", 15f, cyan, true)
        nightTotal = label("Noćni rad  ·  0 h", 15f, purple, true)
        hoursCard.addView(dayTotal); hoursCard.addView(space(5)); hoursCard.addView(nightTotal)
        content.addView(hoursCard)
        content.addView(space(14))
        content.addView(label("Noćni rad: 22:00–06:00  •  Podaci se čuvaju na telefonu.", 11f, muted))
    }

    private fun renderCalendar() {
        monthLabel.text = month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("sr", "BA")))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("sr", "BA")) else it.toString() }
        calendar.removeAllViews()
        val days = listOf("P", "U", "S", "Č", "P", "S", "N")
        days.forEach { d ->
            val tv = label(d, 12f, muted, true).apply { gravity = Gravity.CENTER }
            calendar.addView(tv, GridLayout.LayoutParams().apply { width = 0; height = dp(30); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) })
        }
        val offset = (month.atDay(1).dayOfWeek.value - 1)
        repeat(offset) { calendar.addView(View(this), GridLayout.LayoutParams().apply { width = 0; height = dp(44); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) }) }
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            val hasData = prefs.contains(key(date))
            val tv = TextView(this).apply {
                text = if (hasData) "$day\n•" else day.toString()
                textSize = if (hasData) 12f else 14f
                gravity = Gravity.CENTER
                setTextColor(if (date == selected) bg else if (hasData) cyan else white)
                background = rounded(if (date == selected) cyan else if (hasData) Color.rgb(35, 48, 68) else panel, 12)
                setOnClickListener {
                    note = noteInput.text.toString()
                    selected = date
                    loadDay()
                    updateTotals()
                    renderCalendar()
                }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0; height = dp(44); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }
            calendar.addView(tv, lp)
        }
        animateIn(calendar)
    }

    private fun loadDay() {
        val data = try { JSONObject(prefs.getString(key(selected), "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        start = data.optString("start", "08:00")
        end = data.optString("end", "16:00")
        note = data.optString("note", "")
        if (::startButton.isInitialized) {
            startButton.text = start; endButton.text = end; noteInput.setText(note)
            selectedLabel.text = selected.format(DateTimeFormatter.ofPattern("d. MMMM yyyy.", Locale("sr", "BA")))
        }
    }

    private fun saveDay() {
        val data = JSONObject().put("start", start).put("end", end).put("note", note)
        prefs.edit().putString(key(selected), data.toString()).apply()
    }

    private fun updateTotals() {
        if (!::dayTotal.isInitialized) return
        selectedLabel.text = selected.format(DateTimeFormatter.ofPattern("d. MMMM yyyy.", Locale("sr", "BA")))
        val current = splitHours(start, end)
        dayTotal.text = "Dnevni rad  ·  ${fmt(current.first)}"
        nightTotal.text = "Noćni rad  ·  ${fmt(current.second)}"
        var daySum = 0; var nightSum = 0
        for (d in 1..month.lengthOfMonth()) {
            val date = month.atDay(d)
            val obj = try { JSONObject(prefs.getString(key(date), "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
            val s = obj.optString("start", "")
            val e = obj.optString("end", "")
            if (s.isNotBlank() && e.isNotBlank()) {
                val split = splitHours(s, e); daySum += split.first; nightSum += split.second
            }
        }
        monthTotals.text = "☀  Dnevni: ${fmt(daySum)}       ☾  Noćni: ${fmt(nightSum)}\nUkupno: ${fmt(daySum + nightSum)}"
    }

    // Intersects a shift with the daily 06:00–22:00 and night 22:00–06:00 periods.
    private fun splitHours(startText: String, endText: String): Pair<Int, Int> {
        fun mins(s: String) = s.substringBefore(":").toInt() * 60 + s.substringAfter(":").toInt()
        val s = mins(startText)
        var e = mins(endText)
        if (e <= s) e += 1440
        var night = 0
        for (base in listOf(0, 1440)) {
            night += overlap(s, e, base, base + 360)
            night += overlap(s, e, base + 1320, base + 1440)
        }
        val total = e - s
        night = night.coerceIn(0, total)
        return Pair(total - night, night)
    }
    private fun overlap(a: Int, b: Int, c: Int, d: Int) = (minOf(b, d) - maxOf(a, c)).coerceAtLeast(0)
    private fun fmt(minutes: Int) = "${minutes / 60} h ${minutes % 60} min"
    private fun key(date: LocalDate) = "day_${date}"
    private fun chooseTime(isStart: Boolean) {
        val current = if (isStart) start else end
        TimePickerDialog(this, { _, h, m ->
            val value = String.format(Locale.US, "%02d:%02d", h, m)
            if (isStart) { start = value; startButton.text = value } else { end = value; endButton.text = value }
            updateTotals()
        }, current.substringBefore(":").toInt(), current.substringAfter(":").toInt(), true).show()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(3), 0, dp(3))
    }
    private fun button(text: String, color: Int) = Button(this).apply {
        this.text = text; textSize = 14f; isAllCaps = false; setTextColor(if (color == cyan) bg else white)
        setTypeface(typeface, Typeface.BOLD); background = rounded(color, 14)
    }
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14))
        background = rounded(panel, 20)
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun animateIn(view: View) {
        view.alpha = 0f; view.translationY = dp(7).toFloat()
        view.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
    }
}
