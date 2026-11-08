package com.example.ui

import android.content.Context
import java.util.Locale

/**
 * Camada de tradução para as opções do criador de personagem.
 *
 * PRINCÍPIO: o CharacterConfig guarda e envia SEMPRE o valor inglês
 * (que é o que o workflow GitHub aceita). Esta classe apenas converte
 * entre o valor interno (EN) e o label visível no UI (língua do dispositivo).
 *
 * Uso:
 *   val t = CharacterTranslations.get(context)
 *   // Mostrar ao utilizador:
 *   t.label("The Soldier")  →  "O Soldado"  (em PT)
 *   // Quando selecciona, guardar o valor original:
 *   t.value("O Soldado")    →  "The Soldier"
 */
object CharacterTranslations {

    // ── Detecção de língua ────────────────────────────────────────────────────

    fun isPortuguese(context: Context): Boolean {
        val lang = Locale.getDefault().language
        return lang == "pt"
    }

    fun get(context: Context): Translations =
        if (isPortuguese(context)) ptTranslations else enTranslations

    // ── Classe de tradução ────────────────────────────────────────────────────

    class Translations(private val map: Map<String, String>) {
        // valor EN → label visível
        fun label(enValue: String): String = map[enValue] ?: enValue
        // label visível → valor EN (para guardar no CharacterConfig)
        fun value(label: String): String = map.entries.firstOrNull { it.value == label }?.key ?: label
        // converter lista EN para lista de labels
        fun labels(enList: List<String>): List<String> = enList.map { label(it) }
    }

    // ── Inglês (identidade — sem tradução) ───────────────────────────────────

    val enTranslations = Translations(emptyMap())

    // ── Português ─────────────────────────────────────────────────────────────

    val ptTranslations = Translations(mapOf(

        // ── Presets ──────────────────────────────────────────────────────────
        "Athletic man"                  to "Homem Atlético",
        "Athletic woman"                to "Mulher Atlética",
        "The Soldier"                   to "O Soldado",
        "The African Warrior"           to "O Guerreiro Africano",
        "The Yoruba Warrior"            to "O Guerreiro Yoruba",
        "The Maasai Warrior"            to "O Guerreiro Maasai",
        "The Zulu Warrior"              to "O Guerreiro Zulu",
        "The Dahomey Warrior"           to "O Guerreiro do Daomé",
        "The American Sheriff"          to "O Xerife Americano",
        "The Police Officer (Nigeria)"  to "Polícia (Nigéria)",
        "The FRSC Officer (Nigeria)"    to "Agente FRSC (Nigéria)",
        "Detective"                     to "Detetive",
        "The Secret Agent"              to "O Agente Secreto",
        "The Covert Spy"                to "O Espião Encoberto",
        "The Professional Hitman"       to "O Assassino Profissional",
        "The Bodyguard"                 to "O Guarda-costas",
        "The Gangster"                  to "O Gangster",
        "The Prison Escapee"            to "O Fugitivo da Prisão",
        "The Priest"                    to "O Padre",
        "The Corporate High-Flier"      to "O Executivo de Topo",
        "The Female Executive"          to "A Executiva",
        "Businesswoman"                 to "Empresária",
        "The Arabian Oil Tycoon"        to "O Magnata Árabe do Petróleo",
        "The Miami Dealer"              to "O Dealer de Miami",
        "The Adventurer"                to "O Aventureiro",
        "The Cyberpunk Hacker"          to "O Hacker Cyberpunk",
        "The Wasteland Survivor"        to "O Sobrevivente das Ruínas",
        "The Bride"                     to "A Noiva",
        "The Beach Girl"                to "A Rapariga da Praia",
        "Heavy brute"                   to "Bruto Pesado",
        "Elder"                         to "Ancião",
        "Teen"                          to "Adolescente",

        // ── Género/Idade ──────────────────────────────────────────────────────
        "male:18"           to "Masculino: 18 anos",
        "male:25"           to "Masculino: 25 anos",
        "male:30"           to "Masculino: 30 anos",
        "male:40"           to "Masculino: 40 anos",
        "male:50"           to "Masculino: 50 anos",
        "male:60"           to "Masculino: 60 anos",
        "male:70"           to "Masculino: 70 anos",
        "female:18"         to "Feminino: 18 anos",
        "female:25"         to "Feminino: 25 anos",
        "female:30"         to "Feminino: 30 anos",
        "female:40"         to "Feminino: 40 anos",
        "female:50"         to "Feminino: 50 anos",
        "female:60"         to "Feminino: 60 anos",
        "androgynous:25"    to "Andrógino: 25 anos",
        "androgynous:30"    to "Andrógino: 30 anos",

        // ── Corpo ─────────────────────────────────────────────────────────────
        "slim:very slim:short"          to "Magro · Muito magro · Baixo",
        "slim:slim:average"             to "Magro · Magro · Médio",
        "medium:average:average"        to "Médio · Médio · Médio",
        "athletic:average:tall"         to "Atlético · Médio · Alto",
        "athletic:slim:average"         to "Atlético · Magro · Médio",
        "muscular:average:tall"         to "Musculado · Médio · Alto",
        "muscular:heavy:tall"           to "Musculado · Pesado · Alto",
        "very muscular:heavy:very tall" to "Muito Musculado · Pesado · Muito Alto",
        "medium:heavy:average"          to "Médio · Pesado · Médio",
        "medium:very heavy:short"       to "Médio · Muito Pesado · Baixo",
        "none:slim:very short"          to "Sem músculo · Magro · Muito Baixo",

        // ── Tom de pele ───────────────────────────────────────────────────────
        "very light"    to "Muito Clara",
        "light"         to "Clara",
        "mixed"         to "Mista",
        "dark"          to "Escura",
        "very dark"     to "Muito Escura",
        "african"       to "Africana",
        "asian"         to "Asiática",
        "caucasian"     to "Caucasiana",

        // ── Cabelo ────────────────────────────────────────────────────────────
        "none:black"        to "Careca · Preto",
        "buzz:black"        to "Rapado · Preto",
        "buzz:brown"        to "Rapado · Castanho",
        "short02:black"     to "Curto · Preto",
        "short02:brown"     to "Curto · Castanho",
        "short02:blonde"    to "Curto · Louro",
        "short02:grey"      to "Curto · Grisalho",
        "short04:black"     to "Curto ondulado · Preto",
        "short04:brown"     to "Curto ondulado · Castanho",
        "long01:black"      to "Comprido · Preto",
        "long01:brown"      to "Comprido · Castanho",
        "long01:blonde"     to "Comprido · Louro",
        "long01:red"        to "Comprido · Ruivo",
        "long01:purple"     to "Comprido · Roxo",
        "bob02:black"       to "Bob · Preto",
        "bob02:brown"       to "Bob · Castanho",
        "afro01:black"      to "Afro · Preto",
        "cornrows:black"    to "Tranças rastas · Preto",
        "box_braids:black"  to "Box braids · Preto",
        "ponytail01:black"  to "Rabo de cavalo · Preto",
        "ponytail01:brown"  to "Rabo de cavalo · Castanho",
        "braid01:black"     to "Trança · Preto",
        "mohawk:black"      to "Moicano · Preto",
        "mohawk:purple"     to "Moicano · Roxo",

        // ── Pelo facial ───────────────────────────────────────────────────────
        "none"              to "Nenhum",
        "stubble"           to "Barba por fazer",
        "mustache"          to "Bigode",
        "mustache_stubble"  to "Bigode + Barba por fazer",
        "goatee"            to "Pera",
        "short_beard"       to "Barba curta",
        "full_beard"        to "Barba completa",

        // ── Olhos ─────────────────────────────────────────────────────────────
        "blackbrown"    to "Castanho-escuro",
        "darkbrown"     to "Castanho escuro",
        "brown"         to "Castanho",
        "brownlight"    to "Castanho claro",
        "green"         to "Verde",
        "lightblue"     to "Azul claro",
        "grey"          to "Cinzento",
        "ice"           to "Gelo",

        // ── Outfit ────────────────────────────────────────────────────────────
        "preset_default"                to "Padrão do preset",
        "male_casualsuit01"             to "Casual masculino",
        "male_elegantsuit01"            to "Fato elegante masculino",
        "male_worksuit01"               to "Fato de trabalho masculino",
        "female_casualsuit02"           to "Casual feminino",
        "female_elegantsuit01"          to "Fato elegante feminino",
        "female_sportsuit01"            to "Fato desportivo feminino",
        "set_suit_tie"                  to "Fato com gravata",
        "set_wedding_gown"              to "Vestido de noiva",
        "set_cocktail_dress"            to "Vestido de cocktail",
        "set_pantsuit"                  to "Fato feminino calça",
        "set_prison_jumpsuit"           to "Fato de preso",
        "set_street_tee_jeans"          to "T-shirt + Jeans",
        "set_clergy"                    to "Hábito religioso",
        "set_thobe"                     to "Thobe (túnica árabe)",
        "fatigue_shirt+fatigue_pants"   to "Camisa de farda + Calças de farda",
        "uniform_shirt+uniform_trousers" to "Camisa de uniforme + Calças de uniforme",
        "tank_top+cargo_shorts"         to "Camisola de alças + Calções cargo",
        "techwear_jacket+techwear_pants" to "Casaco techwear + Calças techwear",
        // "none" já está mapeado acima

        // ── Calçado:Chapéu ────────────────────────────────────────────────────
        "preset_default:none"       to "Padrão · Sem chapéu",
        "combat_boots:none"         to "Botas de combate · Sem chapéu",
        "combat_boots:helmet"       to "Botas de combate · Capacete",
        "combat_boots:beret"        to "Botas de combate · Boina",
        "sneakers:baseball_cap"     to "Ténis · Boné",
        "sneakers:none"             to "Ténis · Sem chapéu",
        "oxfords:none"              to "Sapatos Oxford · Sem chapéu",
        "oxfords:fedora01"          to "Sapatos Oxford · Chapéu Fedora",
        "shoes01:none"              to "Sapatos 1 · Sem chapéu",
        "shoes02:none"              to "Sapatos 2 · Sem chapéu",
        "heels:none"                to "Saltos altos · Sem chapéu",
        "sandals:none"              to "Sandálias · Sem chapéu",
        "none:cowboy_hat"           to "Descalço · Chapéu de caubói",
        "shoes03:cowboy_hat"        to "Sapatos 3 · Chapéu de caubói",

        // ── Arma ──────────────────────────────────────────────────────────────
        "pistol"    to "Pistola",
        "rifle"     to "Rifle",
        "shotgun"   to "Espingarda",
        "smg"       to "Metralhadora",
        "knife"     to "Faca",
        "sword"     to "Espada",
        "axe"       to "Machado",
        "bat"       to "Bastão",

        // ── Expressão ─────────────────────────────────────────────────────────
        "neutral"       to "Neutro",
        "happy"         to "Feliz",
        "angry"         to "Raivoso",
        "determined"    to "Determinado",
        "sad"           to "Triste",
        "surprised"     to "Surpreendido",
        "afraid"        to "Com medo",

        // ── Rig/Animação ──────────────────────────────────────────────────────
        "game:yes:A"        to "Game rig · Com animações · Pose A",
        "game:yes:T"        to "Game rig · Com animações · Pose T",
        "game:no:A"         to "Game rig · Sem animações · Pose A",
        "game:no:T"         to "Game rig · Sem animações · Pose T",
        "game_lite:yes:A"   to "Game rig lite · Com animações · Pose A",
        "game_lite:yes:T"   to "Game rig lite · Com animações · Pose T",
        "full:yes:A"        to "Rig completo · Com animações · Pose A",
        "full:yes:T"        to "Rig completo · Com animações · Pose T",
        "none:no:A"         to "Sem rig · Sem animações · Pose A",

        // ── Qualidade ─────────────────────────────────────────────────────────
        "low"       to "Baixa",
        "medium"    to "Média",
        "high"      to "Alta",

        // ── Formatos de exportação ────────────────────────────────────────────
        "glb"               to "GLB",
        "fbx"               to "FBX",
        "obj"               to "OBJ",
        "zip"               to "ZIP (todos)",
        "glb,fbx"           to "GLB + FBX",
        "glb,obj"           to "GLB + OBJ",
        "glb,fbx,obj"       to "GLB + FBX + OBJ",
        "glb,fbx,obj,zip"   to "Todos os formatos"
    ))
}
