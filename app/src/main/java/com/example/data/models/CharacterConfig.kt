package com.example.data.models

import com.squareup.moshi.JsonClass

// ─── Opções reais do workflow ara-axev-crime.yml ──────────────────────────────
// Manter aqui para que qualquer ecrã possa listá-las sem hardcode.

object WorkflowOptions {

    val PRESETS = listOf(
        "Athletic man", "Athletic woman", "The Soldier", "The African Warrior",
        "The Yoruba Warrior", "The Maasai Warrior", "The Zulu Warrior",
        "The Dahomey Warrior", "The American Sheriff", "The Police Officer (Nigeria)",
        "The FRSC Officer (Nigeria)", "Detective", "The Secret Agent", "The Covert Spy",
        "The Professional Hitman", "The Bodyguard", "The Gangster", "The Prison Escapee",
        "The Priest", "The Corporate High-Flier", "The Female Executive", "Businesswoman",
        "The Arabian Oil Tycoon", "The Miami Dealer", "The Adventurer",
        "The Cyberpunk Hacker", "The Wasteland Survivor", "The Bride", "The Beach Girl",
        "Heavy brute", "Elder", "Teen"
    )

    val GENDER_AGE = listOf(
        "male:18", "male:25", "male:30", "male:40", "male:50", "male:60", "male:70",
        "female:18", "female:25", "female:30", "female:40", "female:50", "female:60",
        "androgynous:25", "androgynous:30"
    )

    val BODY = listOf(
        "slim:very slim:short", "slim:slim:average", "medium:average:average",
        "athletic:average:tall", "athletic:slim:average", "muscular:average:tall",
        "muscular:heavy:tall", "very muscular:heavy:very tall", "medium:heavy:average",
        "medium:very heavy:short", "none:slim:very short"
    )

    val SKIN_TONE = listOf(
        "very light", "light", "mixed", "dark", "very dark", "african", "asian", "caucasian"
    )

    val HAIR = listOf(
        "none:black", "buzz:black", "buzz:brown", "short02:black", "short02:brown",
        "short02:blonde", "short02:grey", "short04:black", "short04:brown",
        "long01:black", "long01:brown", "long01:blonde", "long01:red", "long01:purple",
        "bob02:black", "bob02:brown", "afro01:black", "cornrows:black",
        "box_braids:black", "ponytail01:black", "ponytail01:brown", "braid01:black",
        "mohawk:black", "mohawk:purple"
    )

    val FACIAL_HAIR = listOf(
        "none", "stubble", "mustache", "mustache_stubble", "goatee", "short_beard", "full_beard"
    )

    val EYES = listOf(
        "blackbrown", "darkbrown", "brown", "brownlight", "green", "lightblue", "grey", "ice"
    )

    val OUTFIT = listOf(
        "preset_default", "male_casualsuit01", "male_elegantsuit01", "male_worksuit01",
        "female_casualsuit02", "female_elegantsuit01", "female_sportsuit01",
        "set_suit_tie", "set_wedding_gown", "set_cocktail_dress", "set_pantsuit",
        "set_prison_jumpsuit", "set_street_tee_jeans", "set_clergy", "set_thobe",
        "fatigue_shirt+fatigue_pants", "uniform_shirt+uniform_trousers",
        "tank_top+cargo_shorts", "techwear_jacket+techwear_pants", "none"
    )

    val SHOES_HAT = listOf(
        "preset_default:none", "combat_boots:none", "combat_boots:helmet",
        "combat_boots:beret", "sneakers:baseball_cap", "sneakers:none",
        "oxfords:none", "oxfords:fedora01", "shoes01:none", "shoes02:none",
        "heels:none", "sandals:none", "none:cowboy_hat", "shoes03:cowboy_hat"
    )

    val WEAPON = listOf(
        "none", "pistol", "rifle", "shotgun", "smg", "knife", "sword", "axe", "bat"
    )

    val EXPRESSION = listOf(
        "neutral", "happy", "angry", "determined", "sad", "surprised", "afraid"
    )

    val RIG_ANIM = listOf(
        "game:yes:A", "game:yes:T", "game:no:A", "game:no:T",
        "game_lite:yes:A", "game_lite:yes:T", "full:yes:A", "full:yes:T", "none:no:A"
    )

    val QUALITY = listOf("low", "medium", "high")

    val EXPORT_FORMATS = listOf(
        "glb", "fbx", "obj", "zip",
        "glb,fbx", "glb,obj", "glb,fbx,obj", "glb,fbx,obj,zip"
    )
}

// ─── Modelo principal ─────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CharacterConfig(
    val id: String = "character_${System.currentTimeMillis()}",
    val name: String = "AXEV Character",

    // Mapeados directamente para os inputs do workflow
    val preset: String = "The Soldier",
    val genderAge: String = "male:25",
    val body: String = "medium:average:average",
    val skinTone: String = "mixed",
    val hair: String = "short02:black",
    val facialHair: String = "none",
    val eyes: String = "brown",
    val outfit: String = "preset_default",
    val shoesHat: String = "preset_default:none",
    val weapon: String = "none",
    val expression: String = "neutral",
    val rigAnim: String = "game:yes:A",
    val quality: String = "high",
    val exportFormats: String = "glb",

    // Campos legados mantidos para compatibilidade
    val identity: Identity = Identity(),
    val appearance: Appearance = Appearance(),
    val clothing: Clothing = Clothing(),
    val style: VisualStyle = VisualStyle(),
    val export: ExportConfig = ExportConfig()
)

// ─── Modelos de suporte (legados, mantidos) ───────────────────────────────────

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
    val type: String = "Military",
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
