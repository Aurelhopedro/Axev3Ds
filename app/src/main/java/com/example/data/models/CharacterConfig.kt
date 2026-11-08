package com.example.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CharacterConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val identity: Identity = Identity(),
    val appearance: Appearance = Appearance(),
    val clothing: Clothing = Clothing(),
    val style: VisualStyle = VisualStyle(),
    val export: ExportConfig = ExportConfig()
)

@JsonClass(generateAdapter = true)
data class Identity(
    val gender: String = "Male",
    val age: String = "25",
    val category: String = "Soldier",
    val personality: String = "Disciplined",
    val history: String = ""
)

@JsonClass(generateAdapter = true)
data class Appearance(
    val bodyType: String = "Normal",
    val height: Float = 1.0f,
    val shoulderWidth: Float = 1.0f,
    val muscleMass: Float = 1.0f,
    val skinTone: String = "Light",
    val hairStyle: String = "Short",
    val hairColor: String = "Black",
    val beardStyle: String = "None"
)

@JsonClass(generateAdapter = true)
data class Clothing(
    val type: String = "Military", // Military, Police, Civil
    val subType: String = "Camouflage Green",
    val equipment: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VisualStyle(
    val direction: String = "Realist",
    val quality: String = "High"
)

@JsonClass(generateAdapter = true)
data class ExportConfig(
    val formats: List<String> = listOf("GLB"),
    val includeRig: Boolean = true,
    val includeAnimations: Boolean = true
)
