package com.nooralislam.app.utils

import com.nooralislam.app.data.PrayerTimesModel
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

object PrayerQiblaMath {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LNG = 39.826206

    fun qiblaBearing(lat: Double, lng: Double): Double {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dLon = Math.toRadians(KAABA_LNG - lng)
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) % 360.0) + 360.0) % 360.0
    }

    fun calculateTimes(
        lat: Double,
        lng: Double,
        tzHours: Double = TimeZone.getDefault().rawOffset / 3_600_000.0,
        fajrAngle: Double = 19.5,
        ishaAngle: Double = 17.5
    ): PrayerTimesModel {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        var y = year
        var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5

        val d = jd - 2451545.0
        val g = Math.toRadians((357.529 + 0.98560028 * d).mod(360.0))
        val q = (280.459 + 0.98564736 * d).mod(360.0)
        val l = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)).mod(360.0))
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        val decl = asin(sin(e) * sin(l))
        var ra = Math.toDegrees(atan2(cos(e) * sin(l), cos(l))) / 15.0
        ra = ra.mod(24.0)
        var eqTime = (q / 15.0 - ra)
        if (eqTime > 12) eqTime -= 24
        if (eqTime < -12) eqTime += 24

        val dhuhr = 12.0 + tzHours - lng / 15.0 - eqTime
        val phi = Math.toRadians(lat)

        fun sunAngleOffset(angleDeg: Double): Double {
            val cosH = (sin(Math.toRadians(angleDeg)) - sin(phi) * sin(decl)) / (cos(phi) * cos(decl))
            return Math.toDegrees(acos(cosH.coerceIn(-1.0, 1.0))) / 15.0
        }

        val sunriseOff = sunAngleOffset(-0.833)
        val fajrOff = sunAngleOffset(-fajrAngle)
        val ishaOff = sunAngleOffset(-ishaAngle)

        val asrAlt = atan(1.0 / (1.0 + tan(abs(phi - decl))))
        val asrCosH = (sin(asrAlt) - sin(phi) * sin(decl)) / (cos(phi) * cos(decl))
        val asrOff = Math.toDegrees(acos(asrCosH.coerceIn(-1.0, 1.0))) / 15.0

        return PrayerTimesModel(
            fajr = dhuhr - fajrOff,
            sunrise = dhuhr - sunriseOff,
            dhuhr = dhuhr,
            asr = dhuhr + asrOff,
            maghrib = dhuhr + sunriseOff,
            isha = dhuhr + ishaOff
        )
    }

    fun formatTime(hours: Double): String {
        val totalMin = (hours * 60).roundToInt().mod(24 * 60)
        val h24 = totalMin / 60
        val min = totalMin % 60
        val period = if (h24 >= 12) "م" else "ص"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return "%d:%02d %s".format(h12, min, period)
    }
}
