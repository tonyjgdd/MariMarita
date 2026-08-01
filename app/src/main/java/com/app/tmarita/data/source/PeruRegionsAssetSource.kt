package com.app.tmarita.data.source

import android.content.Context
import com.app.tmarita.model.PeruRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class PeruMapGeometry(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val regions: List<PeruRegion>
)

/**
 * Fuente de datos ESTÁTICA: la geometría del mapa no cambia,
 * así que la leemos una vez de /assets/peru_regions.json.
 */
@Singleton
class PeruRegionsAssetSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cached: PeruMapGeometry? = null

    fun load(assetName: String = "peru_regions.json"): PeruMapGeometry {
        cached?.let { return it }

        val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val regionsArray = root.getJSONArray("regions")
        val regions = (0 until regionsArray.length()).map { i ->
            val obj = regionsArray.getJSONObject(i)
            PeruRegion(
                id = obj.getString("id"),
                title = obj.getString("title"),
                pathData = obj.getString("pathData")
            )
        }
        return PeruMapGeometry(
            viewportWidth = root.getDouble("viewportWidth").toFloat(),
            viewportHeight = root.getDouble("viewportHeight").toFloat(),
            regions = regions
        ).also { cached = it }
    }

    companion object {
        // Ids que no cuentan como "departamento visitable" (lago, entidades especiales).
        val NON_VISITABLE_IDS = setOf("PE-LKT")

        // Lima (departamento) + El Callao + Municipalidad Metropolitana de Lima
        // se tratan como UN SOLO "departamento" fijo: siempre visitado, no se puede desmarcar.
        // Se siguen dibujando como 3 formas distintas en el mapa, pero cuentan como 1 en los totales.
        val LIMA_GROUP_IDS = setOf("PE-LIM", "PE-CAL", "PE-LMA")
        const val LIMA_GROUP_LABEL = "Lima"
    }
}