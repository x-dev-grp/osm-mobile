package com.xdev.osm_mobile.models

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
sealed class ArticleConfig {
    abstract val configType: String
}
data class UniteConfig(
    @SerializedName("configType") override val configType: String = "UNITE",
    @SerializedName("material") val material: String?,
    @SerializedName("volumeMl") val volumeMl: Int?,
    @SerializedName("color") val color: String?,
    @SerializedName("neckType") val neckType: String?,
    @SerializedName("weightGr") val weightGr: Int?
) : ArticleConfig()
data class ColisConfig(
    @SerializedName("configType") override val configType: String = "COLIS",
    @SerializedName("unitArticleId") val unitArticleId: String?,
    @SerializedName("unitsPerColis") val unitsPerColis: Int?,
    @SerializedName("dimensions") val dimensions: Dimensions?,
    @SerializedName("maxWeightKg") val maxWeightKg: Double?
) : ArticleConfig()
data class Dimensions(
    @SerializedName("length") val length: Double?,
    @SerializedName("width") val width: Double?,
    @SerializedName("height") val height: Double?
)
data class PaletteConfig(
    @SerializedName("configType") override val configType: String = "PALETTE",
    @SerializedName("type") val type: String?,
    @SerializedName("material") val material: String?,
    @SerializedName("colisPerLayer") val colisPerLayer: Int?,
    @SerializedName("numberOfLayers") val numberOfLayers: Int?,
    @SerializedName("maxHeightCm") val maxHeightCm: Int?,
    @SerializedName("clientSpecific") val clientSpecific: Boolean?,
    @SerializedName("colisId") val colisId: String?
) : ArticleConfig()
data class EmballageConfig(
    @SerializedName("configType") override val configType: String = "EMBALLAGE",
    @SerializedName("sousType") val sousType: String?,
    @SerializedName("material") val material: String?,
    @SerializedName("dimensions") val dimensions: Dimensions?,
    @SerializedName("clientBranding") val clientBranding: Boolean?,
    @SerializedName("poidsGrammes") val poidsGrammes: Double?
) : ArticleConfig()
data class ConsommableConfig(
    @SerializedName("configType") override val configType: String = "CONSOMMABLE",
    @SerializedName("sousType") val sousType: String?,
    @SerializedName("usage") val usage: String?,
    @SerializedName("unit") val unit: String?,
    @SerializedName("quantity") val quantity: Double?,
    @SerializedName("temperatureStockageCelsius") val temperatureStockageCelsius: Int?
) : ArticleConfig()
data class MatierePremiereConfig(
    @SerializedName("configType") override val configType: String = "MATIERE_PREMIERE",
    @SerializedName("sousType") val sousType: String?,
    @SerializedName("origin") val origin: String?,
    @SerializedName("qualityGrade") val qualityGrade: String?,
    @SerializedName("density") val density: Double?,
    @SerializedName("certifieBio") val certifieBio: Boolean?
) : ArticleConfig()
data class AccessoireConfig(
    @SerializedName("configType") override val configType: String = "ACCESSOIRE",
    @SerializedName("sousType") val sousType: String?,
    @SerializedName("usage") val usage: String?,
    @SerializedName("necessiteMontage") val necessiteMontage: Boolean?,
    @SerializedName("garantieMois") val garantieMois: Int?
) : ArticleConfig()
class ArticleConfigDeserializer : JsonDeserializer<ArticleConfig> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ArticleConfig {
        val jsonObject = json.asJsonObject
        val configType = jsonObject.get("configType")?.asString
        return when (configType) {
            "UNITE" -> context.deserialize(json, UniteConfig::class.java)
            "COLIS" -> context.deserialize(json, ColisConfig::class.java)
            "PALETTE" -> context.deserialize(json, PaletteConfig::class.java)
            "EMBALLAGE" -> context.deserialize(json, EmballageConfig::class.java)
            "CONSOMMABLE" -> context.deserialize(json, ConsommableConfig::class.java)
            "MATIERE_PREMIERE" -> context.deserialize(json, MatierePremiereConfig::class.java)
            "ACCESSOIRE" -> context.deserialize(json, AccessoireConfig::class.java)
            else -> throw JsonParseException("Type de configuration inconnu: $configType")
        }
    }
}
