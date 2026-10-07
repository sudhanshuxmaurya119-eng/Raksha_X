package com.rakshax.app.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.rakshax.app.data.model.FacilityType
import com.rakshax.app.data.model.RiskLevel

object MapMarkerUtils {

    private val cache = mutableMapOf<String, BitmapDescriptor>()

    fun getFacilityMarker(context: Context, type: FacilityType): BitmapDescriptor {
        val key = "fac_${type.name}"
        cache[key]?.let { return it }

        val (primaryColor, symbol) = when (type) {
            FacilityType.POLICE -> Pair(AndroidColor.rgb(24, 119, 242), "🚓") // Royal Blue
            FacilityType.NGO -> Pair(AndroidColor.rgb(0, 168, 89), "🤝")      // Emerald Green
            FacilityType.HOSPITAL -> Pair(AndroidColor.rgb(220, 20, 60), "🏥") // Crimson Red
            FacilityType.FIRE_STATION -> Pair(AndroidColor.rgb(255, 122, 0), "🚒") // Vibrant Amber
            FacilityType.GOV_CENTER -> Pair(AndroidColor.rgb(138, 43, 226), "🏛️") // Slate Purple
        }

        val bitmap = createPinBitmap(context, primaryColor, symbol)
        val desc = BitmapDescriptorFactory.fromBitmap(bitmap)
        cache[key] = desc
        return desc
    }

    fun getCaseMarker(context: Context, severity: String): BitmapDescriptor {
        val key = "case_$severity"
        cache[key]?.let { return it }

        val color = when (severity.uppercase()) {
            "HIGH" -> AndroidColor.rgb(220, 20, 60)
            "MEDIUM" -> AndroidColor.rgb(255, 179, 0)
            else -> AndroidColor.rgb(76, 175, 80)
        }

        val bitmap = createPinBitmap(context, color, "⚠️")
        val desc = BitmapDescriptorFactory.fromBitmap(bitmap)
        cache[key] = desc
        return desc
    }

    fun getUserLocationMarker(context: Context): BitmapDescriptor {
        val key = "user_loc"
        cache[key]?.let { return it }

        val density = context.resources.displayMetrics.density
        val size = (36 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = size / 2f
        val outerRadius = 16f * density
        val innerRadius = 8f * density
        val haloRadius = 14f * density

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer glow halo
        paint.color = AndroidColor.argb(70, 0, 150, 255)
        canvas.drawCircle(center, center, outerRadius, paint)

        // White border
        paint.color = AndroidColor.WHITE
        canvas.drawCircle(center, center, haloRadius, paint)

        // Cyan/Blue core
        paint.color = AndroidColor.rgb(0, 122, 255)
        canvas.drawCircle(center, center, innerRadius, paint)

        val desc = BitmapDescriptorFactory.fromBitmap(bitmap)
        cache[key] = desc
        return desc
    }

    fun getClusterMarker(context: Context, count: Int, primaryColor: Int): BitmapDescriptor {
        val key = "cluster_${count}_$primaryColor"
        cache[key]?.let { return it }

        val density = context.resources.displayMetrics.density
        val size = (44 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Translucent background
        paint.color = AndroidColor.argb(80, AndroidColor.red(primaryColor), AndroidColor.green(primaryColor), AndroidColor.blue(primaryColor))
        canvas.drawCircle(center, center - 2 * density, paint)

        // Solid circular badge
        paint.color = primaryColor
        canvas.drawCircle(center, center - 6 * density, paint)

        // White ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f * density
        paint.color = AndroidColor.WHITE
        canvas.drawCircle(center, center - 6 * density, paint)

        // Count text
        paint.style = Paint.Style.FILL
        paint.color = AndroidColor.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f * density
        paint.typeface = Typeface.DEFAULT_BOLD

        val yPos = center - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(count.toString(), center, yPos, paint)

        val desc = BitmapDescriptorFactory.fromBitmap(bitmap)
        cache[key] = desc
        return desc
    }

    private fun createPinBitmap(context: Context, color: Int, symbol: String): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (38 * density).toInt()
        val height = (48 * density).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw pin teardrop body
        val path = Path()
        val radius = 16f * density
        val centerX = width / 2f
        val centerY = 18f * density
        val tipY = height - 2f * density

        path.addCircle(centerX, centerY, radius, Path.Direction.CW)

        val pointer = Path().apply {
            moveTo(centerX - 10f * density, centerY + 8f * density)
            lineTo(centerX, tipY)
            lineTo(centerX + 10f * density, centerY + 8f * density)
            close()
        }
        path.op(pointer, Path.Op.UNION)

        // Shadow / Outer outline
        paint.color = AndroidColor.argb(100, 0, 0, 0)
        canvas.drawCircle(centerX, tipY, 4f * density, paint)

        // Pin body
        paint.color = color
        canvas.drawPath(path, paint)

        // White border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.color = AndroidColor.WHITE
        canvas.drawPath(path, paint)

        // Inner white circle for symbol
        paint.style = Paint.Style.FILL
        paint.color = AndroidColor.WHITE
        canvas.drawCircle(centerX, centerY, 11f * density, paint)

        // Symbol emoji / text
        paint.color = AndroidColor.BLACK
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f * density
        val textY = centerY - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(symbol, centerX, textY, paint)

        return bitmap
    }
}
