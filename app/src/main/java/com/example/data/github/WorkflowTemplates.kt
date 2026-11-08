package com.example.data.github

import android.util.Base64

/**
 * Conteúdo dos workflows AXEV embutido na app.
 * Quando um utilizador cria um repositório novo, estes ficheiros são
 * injectados automaticamente em .github/workflows/ via GitHub API.
 *
 * NUNCA incluir build-apk.yml aqui — é privado do autor.
 */
object WorkflowTemplates {

    const val REPO_NAME = "axev-studio"
    const val REPO_DESCRIPTION = "AXEV 3D Studio — gerador de personagens"

    const val WORKFLOW_GENERATE = "ara-axev-crime.yml"
    const val WORKFLOW_SMOKE    = "axev-smoke-test.yml"

    // ── Workflow de geração de personagem ─────────────────────────────────────

    private val GENERATE_CONTENT = """
name: AXEV 3D — Gerar Personagem

on:
  workflow_dispatch:
    inputs:

      character_name:
        description: "Nome do personagem"
        required: true
        type: string
        default: "AXEV Character"

      preset:
        description: "Preset base"
        required: true
        type: choice
        default: "The Soldier"
        options:
          - "Athletic man"
          - "Athletic woman"
          - "The Soldier"
          - "The African Warrior"
          - "The Yoruba Warrior"
          - "The Maasai Warrior"
          - "The Zulu Warrior"
          - "The Dahomey Warrior"
          - "The American Sheriff"
          - "The Police Officer (Nigeria)"
          - "The FRSC Officer (Nigeria)"
          - "Detective"
          - "The Secret Agent"
          - "The Covert Spy"
          - "The Professional Hitman"
          - "The Bodyguard"
          - "The Gangster"
          - "The Prison Escapee"
          - "The Priest"
          - "The Corporate High-Flier"
          - "The Female Executive"
          - "Businesswoman"
          - "The Arabian Oil Tycoon"
          - "The Miami Dealer"
          - "The Adventurer"
          - "The Cyberpunk Hacker"
          - "The Wasteland Survivor"
          - "The Bride"
          - "The Beach Girl"
          - "Heavy brute"
          - "Elder"
          - "Teen"

      gender_age:
        description: "Género e idade (género:idade)"
        required: true
        type: choice
        default: "male:25"
        options:
          - "male:18"
          - "male:25"
          - "male:30"
          - "male:40"
          - "male:50"
          - "male:60"
          - "male:70"
          - "female:18"
          - "female:25"
          - "female:30"
          - "female:40"
          - "female:50"
          - "female:60"
          - "androgynous:25"
          - "androgynous:30"

      body:
        description: "Corpo (musculatura:peso:altura)"
        required: true
        type: choice
        default: "medium:average:average"
        options:
          - "slim:very slim:short"
          - "slim:slim:average"
          - "medium:average:average"
          - "athletic:average:tall"
          - "athletic:slim:average"
          - "muscular:average:tall"
          - "muscular:heavy:tall"
          - "very muscular:heavy:very tall"
          - "medium:heavy:average"
          - "medium:very heavy:short"
          - "none:slim:very short"

      skin_tone:
        description: "Tom de pele"
        required: true
        type: choice
        default: "mixed"
        options:
          - "very light"
          - "light"
          - "mixed"
          - "dark"
          - "very dark"
          - "african"
          - "asian"
          - "caucasian"

      hair:
        description: "Cabelo (estilo:cor)"
        required: true
        type: choice
        default: "short02:black"
        options:
          - "none:black"
          - "buzz:black"
          - "buzz:brown"
          - "short02:black"
          - "short02:brown"
          - "short02:blonde"
          - "short02:grey"
          - "short04:black"
          - "short04:brown"
          - "long01:black"
          - "long01:brown"
          - "long01:blonde"
          - "long01:red"
          - "long01:purple"
          - "bob02:black"
          - "bob02:brown"
          - "afro01:black"
          - "cornrows:black"
          - "box_braids:black"
          - "ponytail01:black"
          - "ponytail01:brown"
          - "braid01:black"
          - "mohawk:black"
          - "mohawk:purple"

      facial_hair:
        description: "Pelo facial"
        required: true
        type: choice
        default: "none"
        options:
          - "none"
          - "stubble"
          - "mustache"
          - "mustache_stubble"
          - "goatee"
          - "short_beard"
          - "full_beard"

      eyes:
        description: "Cor dos olhos"
        required: true
        type: choice
        default: "brown"
        options:
          - "blackbrown"
          - "darkbrown"
          - "brown"
          - "brownlight"
          - "green"
          - "lightblue"
          - "grey"
          - "ice"

      outfit:
        description: "Roupa"
        required: true
        type: choice
        default: "preset_default"
        options:
          - "preset_default"
          - "male_casualsuit01"
          - "male_elegantsuit01"
          - "male_worksuit01"
          - "female_casualsuit02"
          - "female_elegantsuit01"
          - "female_sportsuit01"
          - "set_suit_tie"
          - "set_wedding_gown"
          - "set_cocktail_dress"
          - "set_pantsuit"
          - "set_prison_jumpsuit"
          - "set_street_tee_jeans"
          - "set_clergy"
          - "set_thobe"
          - "fatigue_shirt+fatigue_pants"
          - "uniform_shirt+uniform_trousers"
          - "tank_top+cargo_shorts"
          - "techwear_jacket+techwear_pants"
          - "none"

      shoes_hat:
        description: "Calçado:Chapéu"
        required: true
        type: choice
        default: "preset_default:none"
        options:
          - "preset_default:none"
          - "combat_boots:none"
          - "combat_boots:helmet"
          - "combat_boots:beret"
          - "sneakers:baseball_cap"
          - "sneakers:none"
          - "oxfords:none"
          - "oxfords:fedora01"
          - "shoes01:none"
          - "shoes02:none"
          - "heels:none"
          - "sandals:none"
          - "none:cowboy_hat"
          - "shoes03:cowboy_hat"

      weapon:
        description: "Arma"
        required: true
        type: choice
        default: "none"
        options:
          - "none"
          - "pistol"
          - "rifle"
          - "shotgun"
          - "smg"
          - "knife"
          - "sword"
          - "axe"
          - "bat"

      expression:
        description: "Expressão facial"
        required: true
        type: choice
        default: "neutral"
        options:
          - "neutral"
          - "happy"
          - "angry"
          - "determined"
          - "sad"
          - "surprised"
          - "afraid"

      rig_anim:
        description: "Rig:Animações:Pose"
        required: true
        type: choice
        default: "game:yes:A"
        options:
          - "game:yes:A"
          - "game:yes:T"
          - "game:no:A"
          - "game:no:T"
          - "game_lite:yes:A"
          - "game_lite:yes:T"
          - "full:yes:A"
          - "full:yes:T"
          - "none:no:A"

      quality:
        description: "Qualidade"
        required: true
        type: choice
        default: "high"
        options:
          - "low"
          - "medium"
          - "high"

      export_formats:
        description: "Formatos a exportar"
        required: true
        type: choice
        default: "glb"
        options:
          - "glb"
          - "fbx"
          - "obj"
          - "zip"
          - "glb,fbx"
          - "glb,obj"
          - "glb,fbx,obj"
          - "glb,fbx,obj,zip"

      character_id:
        description: "ID do personagem (gerado pela app)"
        required: true
        type: string
        default: "character_001"

      config_json:
        description: "JSON do CharacterConfig da app (opcional)"
        required: false
        type: string
        default: ""

permissions:
  contents: read

concurrency:
  group: axev-character-${'$'}{{ github.ref }}-${'$'}{{ inputs.character_id }}
  cancel-in-progress: false

jobs:
  generate-character:
    name: "Gerar personagem"
    runs-on: ubuntu-latest
    timeout-minutes: 60

    steps:
      - name: Validar inputs
        env:
          INPUT_FORMATS: ${'$'}{{ inputs.export_formats }}
          INPUT_ID:      ${'$'}{{ inputs.character_id }}
          INPUT_JSON:    ${'$'}{{ inputs.config_json }}
        run: |
          set -euo pipefail
          if ! echo "${'$'}INPUT_ID" | grep -qE '^[a-zA-Z0-9_\-]{1,80}${'$'}'; then
            echo "::error::character_id inválido: '${'$'}INPUT_ID'"
            exit 1
          fi
          ALLOWED="glb fbx obj zip"
          IFS=',' read -ra FMTS <<< "${'$'}INPUT_FORMATS"
          for fmt in "${'$'}{FMTS[@]}"; do
            fmt_clean=$(echo "${'$'}fmt" | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')
            if ! echo "${'$'}ALLOWED" | grep -qw "${'$'}fmt_clean"; then
              echo "::error::Formato não suportado: '${'$'}fmt_clean'"
              exit 1
            fi
          done
          FORMATS_NORM=$(echo "${'$'}INPUT_FORMATS" | tr '[:upper:]' '[:lower:]' | tr -d ' ')
          echo "FORMATS_NORM=${'$'}FORMATS_NORM" >> "${'$'}GITHUB_ENV"
          if [ -n "${'$'}INPUT_JSON" ]; then
            if ! echo "${'$'}INPUT_JSON" | python3 -c "import sys,json; json.load(sys.stdin)" 2>/dev/null; then
              echo "::error::config_json contém JSON malformado."
              exit 1
            fi
          fi
          echo "✅ Inputs validados. Formatos: ${'$'}FORMATS_NORM"

      - name: Checkout AXEV
        uses: actions/checkout@v4

      - name: Configurar Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: "20"

      - name: Clonar Ara 3D Gen
        run: git clone --depth 1 https://github.com/olatunde85/3d-gen.git ara-3d-gen

      - name: Instalar dependências
        working-directory: ara-3d-gen
        run: |
          if [ -f package-lock.json ]; then npm ci; else npm install; fi

      - name: Cache assets MakeHuman
        id: cache-assets
        uses: actions/cache@v4
        with:
          path: |
            ara-3d-gen/vendor
            ara-3d-gen/public/assets
          key: ara3d-assets-v1-${'$'}{{ hashFiles('ara-3d-gen/scripts/setup-assets.ts', 'ara-3d-gen/scripts/mocap-takes.ts') }}
          restore-keys: ara3d-assets-v1-

      - name: Preparar assets 3D
        working-directory: ara-3d-gen
        run: |
          if [ "${'$'}{{ steps.cache-assets.outputs.cache-hit }}" != "true" ] || [ ! -d "vendor/sys/skins" ]; then
            echo "A descarregar assets MakeHuman (~280 MB)..."
            npm run setup-assets
          else
            echo "✅ Assets em cache."
          fi

      - name: Construir parâmetros do personagem
        env:
          INPUT_PRESET:      ${'$'}{{ inputs.preset }}
          INPUT_NAME:        ${'$'}{{ inputs.character_name }}
          INPUT_GENDER_AGE:  ${'$'}{{ inputs.gender_age }}
          INPUT_BODY:        ${'$'}{{ inputs.body }}
          INPUT_SKIN:        ${'$'}{{ inputs.skin_tone }}
          INPUT_HAIR:        ${'$'}{{ inputs.hair }}
          INPUT_FACIAL_HAIR: ${'$'}{{ inputs.facial_hair }}
          INPUT_EYES:        ${'$'}{{ inputs.eyes }}
          INPUT_OUTFIT:      ${'$'}{{ inputs.outfit }}
          INPUT_SHOES_HAT:   ${'$'}{{ inputs.shoes_hat }}
          INPUT_WEAPON:      ${'$'}{{ inputs.weapon }}
          INPUT_EXPRESSION:  ${'$'}{{ inputs.expression }}
          INPUT_RIG_ANIM:    ${'$'}{{ inputs.rig_anim }}
          INPUT_QUALITY:     ${'$'}{{ inputs.quality }}
          INPUT_JSON:        ${'$'}{{ inputs.config_json }}
        run: |
          mkdir -p axev-build
          python3 << 'PYEOF'
          import json, os
          def env(k, d=""): return os.environ.get(k, d).strip()
          preset = env("INPUT_PRESET", "Athletic man")
          params = {"name": env("INPUT_NAME", preset) or preset, "detail": env("INPUT_QUALITY", "high").lower()}
          ga = env("INPUT_GENDER_AGE", "male:25").split(":")
          gender_str = ga[0] if len(ga) > 0 else "male"
          age_str = ga[1] if len(ga) > 1 else "25"
          params["gender"] = 0.0 if gender_str == "female" else (0.5 if gender_str == "androgynous" else 1.0)
          try:
            y = max(12, min(90, int(age_str)))
            params["age"] = round(0.5 + ((y-25)/65)*0.5 if y >= 25 else 0.1875 + ((y-11)/14)*0.3125 if y >= 11 else (y/11)*0.1875, 4)
          except: pass
          muscle_map = {"none":0.1,"slim":0.3,"medium":0.5,"athletic":0.7,"muscular":0.85,"very muscular":1.0}
          weight_map = {"very slim":0.15,"slim":0.3,"average":0.5,"heavy":0.7,"very heavy":0.9}
          height_map = {"very short":0.1,"short":0.3,"average":0.5,"tall":0.7,"very tall":0.9}
          body_parts = env("INPUT_BODY", "medium:average:average").split(":")
          if len(body_parts) >= 1: params["muscle"] = muscle_map.get(body_parts[0], 0.5)
          if len(body_parts) >= 2: params["weight"] = weight_map.get(body_parts[1], 0.5)
          if len(body_parts) >= 3: params["height"] = height_map.get(body_parts[2], 0.5)
          skin_map = {"very light":{"african":0.0,"asian":0.05,"caucasian":0.95},"light":{"african":0.0,"asian":0.1,"caucasian":0.9},"mixed":{"african":0.3,"asian":0.2,"caucasian":0.5},"dark":{"african":0.7,"asian":0.1,"caucasian":0.2},"very dark":{"african":0.9,"asian":0.05,"caucasian":0.05},"african":{"african":1.0,"asian":0.0,"caucasian":0.0},"asian":{"african":0.0,"asian":1.0,"caucasian":0.0},"caucasian":{"african":0.0,"asian":0.0,"caucasian":1.0}}
          params.update(skin_map.get(env("INPUT_SKIN","mixed"), {"african":0.3,"asian":0.2,"caucasian":0.5}))
          hair_parts = env("INPUT_HAIR","short02:black").split(":")
          hair_style = hair_parts[0] if len(hair_parts) > 0 else "short02"
          hair_color = hair_parts[1] if len(hair_parts) > 1 else "black"
          if hair_style != "preset_default": params["hair"] = hair_style
          color_map = {"black":"#15100c","dark brown":"#241a13","brown":"#3b2a1d","blonde":"#c8a86b","red":"#7a2e14","grey":"#6a6060","white":"#c8c0b4","purple":"#4a1a6a","blue":"#1a2a6a","pink":"#c060a0"}
          hc = color_map.get(hair_color)
          if hc: params["hairColor"] = hc
          fh = env("INPUT_FACIAL_HAIR","none")
          if fh != "preset_default": params["facialHair"] = fh
          params["eyes"] = env("INPUT_EYES","brown")
          params["expression"] = env("INPUT_EXPRESSION","neutral")
          w = env("INPUT_WEAPON","none")
          if w not in ("preset_default",""): params["weapon"] = w
          outfit = env("INPUT_OUTFIT","preset_default")
          if outfit == "preset_default": pass
          elif "+" in outfit:
            top, bottom = outfit.split("+", 1)
            params["outfit"] = "none"; params["top"] = top; params["bottom"] = bottom
          elif outfit == "none": params["outfit"] = "none"
          else: params["outfit"] = outfit
          sh = env("INPUT_SHOES_HAT","preset_default:none").split(":")
          shoes = sh[0] if len(sh) > 0 else "preset_default"
          hat = sh[1] if len(sh) > 1 else "none"
          if shoes not in ("preset_default",""): params["shoes"] = shoes
          if hat not in ("preset_default",""): params["hat"] = hat
          ra = env("INPUT_RIG_ANIM","game:yes:A").split(":")
          params["rig"] = ra[0] if len(ra) > 0 else "game"
          params["animations"] = (ra[1] if len(ra) > 1 else "yes") == "yes"
          params["restPose"] = ra[2] if len(ra) > 2 else "A"
          params["faceShapes"] = True; params["speechShapes"] = True; params["faceBones"] = True
          with open("axev-build/character_overrides.json","w") as f: json.dump(params, f, indent=2)
          print("✅ Parâmetros prontos:"); print(json.dumps(params, indent=2))
          PYEOF

      - name: Gerar personagem com Ara 3D Gen
        working-directory: ara-3d-gen
        env:
          FORMATS_NORM:    ${'$'}{{ env.FORMATS_NORM }}
          RESOLVED_PRESET: ${'$'}{{ inputs.preset }}
        run: |
          set -euo pipefail
          mkdir -p out
          cat > merge-params.ts << 'TSEOF'
          import { readFileSync, writeFileSync } from 'node:fs';
          import { DEFAULT_PARAMS, PRESETS } from './src/core/params';
          const presetName = process.env.RESOLVED_PRESET ?? 'Athletic man';
          const preset = (PRESETS as Record<string, object>)[presetName] ?? {};
          const overrides = JSON.parse(readFileSync('../axev-build/character_overrides.json', 'utf8'));
          const final = { ...DEFAULT_PARAMS, ...preset, ...overrides };
          writeFileSync('../axev-build/character.params.json', JSON.stringify(final, null, 2));
          console.log(`✅ Preset "${'$'}{presetName}" + ${'$'}{Object.keys(overrides).length} overrides combinados.`);
          TSEOF
          npx tsx merge-params.ts
          npm run export -- --params "../axev-build/character.params.json" --formats "${'$'}FORMATS_NORM" --out out
          echo "✅ Geração concluída."
          find out -type f -printf '  %p (%s bytes)\n'

      - name: Validar ficheiros exportados
        env:
          FORMATS_NORM: ${'$'}{{ env.FORMATS_NORM }}
        run: |
          set -euo pipefail
          OUT="ara-3d-gen/out"
          if [ ! -d "${'$'}OUT" ] || [ -z "$(find "${'$'}OUT" -type f -print -quit)" ]; then
            echo "::error::Nenhum ficheiro exportado."; exit 1
          fi
          IFS=',' read -ra FMTS <<< "${'$'}FORMATS_NORM"
          for fmt in "${'$'}{FMTS[@]}"; do
            fmt=$(echo "${'$'}fmt" | tr -d '[:space:]')
            FILES=$(find "${'$'}OUT" -name "*.${'$'}{fmt}" -type f 2>/dev/null)
            if [ -z "${'$'}FILES" ]; then
              echo "::error::Formato ${'$'}fmt não produzido."; exit 1
            fi
            for f in ${'$'}FILES; do
              SIZE=$(stat -c%s "${'$'}f")
              echo "✅ ${'$'}fmt: ${'$'}f (${'$'}(( SIZE / 1024 )) KB)"
            done
          done
          echo "✅ Todos os formatos validados."

      - name: Publicar artefacto
        uses: actions/upload-artifact@v4
        with:
          name: AXEV-CHARACTER-${'$'}{{ inputs.character_id }}
          path: ara-3d-gen/out/
          if-no-files-found: error
          retention-days: 14
""".trimIndent()

    // ── Workflow de smoke test ────────────────────────────────────────────────

    private val SMOKE_CONTENT = """
name: AXEV 3D — Smoke Test

on:
  workflow_dispatch:

permissions:
  contents: read

jobs:
  smoke:
    name: "[${'$'}{{ matrix.id }}] ${'$'}{{ matrix.label }}"
    runs-on: ubuntu-latest
    timeout-minutes: 60
    continue-on-error: true
    strategy:
      fail-fast: false
      matrix:
        include:
          - id: "TC01"
            label: "Guerreiro Africano — GLB"
            character_id: "smoke_tc01"
            character_name: "Smoke TC01"
            preset: "The African Warrior"
            gender_age: "male:30"
            body: "muscular:average:tall"
            skin_tone: "african"
            hair: "buzz:black"
            facial_hair: "short_beard"
            eyes: "darkbrown"
            outfit: "preset_default"
            shoes_hat: "combat_boots:none"
            weapon: "none"
            expression: "determined"
            rig_anim: "game:yes:A"
            quality: "high"
            export_formats: "glb"
          - id: "TC02"
            label: "Executiva Asiática — FBX"
            character_id: "smoke_tc02"
            character_name: "Smoke TC02"
            preset: "The Female Executive"
            gender_age: "female:35"
            body: "slim:slim:average"
            skin_tone: "asian"
            hair: "bob02:black"
            facial_hair: "none"
            eyes: "brown"
            outfit: "female_elegantsuit01"
            shoes_hat: "heels:none"
            weapon: "none"
            expression: "neutral"
            rig_anim: "game:no:T"
            quality: "medium"
            export_formats: "fbx"
          - id: "TC03"
            label: "Andrógino sem roupa — GLB"
            character_id: "smoke_tc03"
            character_name: "Smoke TC03"
            preset: "Athletic man"
            gender_age: "androgynous:25"
            body: "athletic:slim:average"
            skin_tone: "mixed"
            hair: "short02:brown"
            facial_hair: "none"
            eyes: "green"
            outfit: "none"
            shoes_hat: "none:none"
            weapon: "none"
            expression: "neutral"
            rig_anim: "none:no:A"
            quality: "low"
            export_formats: "glb"
          - id: "TC04"
            label: "Elder Caucasiano — ZIP"
            character_id: "smoke_tc04"
            character_name: "Smoke TC04"
            preset: "Elder"
            gender_age: "male:70"
            body: "medium:heavy:average"
            skin_tone: "caucasian"
            hair: "short02:grey"
            facial_hair: "full_beard"
            eyes: "grey"
            outfit: "set_clergy"
            shoes_hat: "shoes01:none"
            weapon: "none"
            expression: "sad"
            rig_anim: "full:yes:T"
            quality: "high"
            export_formats: "zip"
          - id: "TC05"
            label: "Agente com arma — GLB+FBX"
            character_id: "smoke_tc05"
            character_name: "Smoke TC05"
            preset: "The Secret Agent"
            gender_age: "male:40"
            body: "athletic:average:tall"
            skin_tone: "dark"
            hair: "short02:black"
            facial_hair: "stubble"
            eyes: "blackbrown"
            outfit: "fatigue_shirt+fatigue_pants"
            shoes_hat: "combat_boots:beret"
            weapon: "pistol"
            expression: "determined"
            rig_anim: "game:yes:A"
            quality: "high"
            export_formats: "glb,fbx"
          - id: "TC06"
            label: "Jovem Feminina — game_lite GLB"
            character_id: "smoke_tc06"
            character_name: "Smoke TC06"
            preset: "The Beach Girl"
            gender_age: "female:18"
            body: "slim:very slim:short"
            skin_tone: "light"
            hair: "long01:blonde"
            facial_hair: "none"
            eyes: "lightblue"
            outfit: "female_sportsuit01"
            shoes_hat: "sneakers:none"
            weapon: "none"
            expression: "happy"
            rig_anim: "game_lite:yes:A"
            quality: "medium"
            export_formats: "glb"
    steps:
      - name: Validar inputs ${'$'}{{ matrix.id }}
        env:
          INPUT_FORMATS: ${'$'}{{ matrix.export_formats }}
          INPUT_ID:      ${'$'}{{ matrix.character_id }}
        run: |
          set -euo pipefail
          FORMATS_NORM=$(echo "${'$'}INPUT_FORMATS" | tr '[:upper:]' '[:lower:]' | tr -d ' ')
          echo "FORMATS_NORM=${'$'}FORMATS_NORM" >> "${'$'}GITHUB_ENV"
      - name: Checkout AXEV
        uses: actions/checkout@v4
      - name: Configurar Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: "20"
      - name: Clonar Ara 3D Gen
        run: git clone --depth 1 https://github.com/olatunde85/3d-gen.git ara-3d-gen
      - name: Instalar dependências
        working-directory: ara-3d-gen
        run: if [ -f package-lock.json ]; then npm ci; else npm install; fi
      - name: Cache assets MakeHuman
        id: cache-assets
        uses: actions/cache@v4
        with:
          path: |
            ara-3d-gen/vendor
            ara-3d-gen/public/assets
          key: ara3d-assets-v1-${'$'}{{ hashFiles('ara-3d-gen/scripts/setup-assets.ts') }}
          restore-keys: ara3d-assets-v1-
      - name: Preparar assets 3D
        working-directory: ara-3d-gen
        run: |
          if [ "${'$'}{{ steps.cache-assets.outputs.cache-hit }}" != "true" ] || [ ! -d "vendor/sys/skins" ]; then
            npm run setup-assets
          fi
      - name: Construir parâmetros ${'$'}{{ matrix.id }}
        env:
          INPUT_PRESET:      ${'$'}{{ matrix.preset }}
          INPUT_NAME:        ${'$'}{{ matrix.character_name }}
          INPUT_GENDER_AGE:  ${'$'}{{ matrix.gender_age }}
          INPUT_BODY:        ${'$'}{{ matrix.body }}
          INPUT_SKIN:        ${'$'}{{ matrix.skin_tone }}
          INPUT_HAIR:        ${'$'}{{ matrix.hair }}
          INPUT_FACIAL_HAIR: ${'$'}{{ matrix.facial_hair }}
          INPUT_EYES:        ${'$'}{{ matrix.eyes }}
          INPUT_OUTFIT:      ${'$'}{{ matrix.outfit }}
          INPUT_SHOES_HAT:   ${'$'}{{ matrix.shoes_hat }}
          INPUT_WEAPON:      ${'$'}{{ matrix.weapon }}
          INPUT_EXPRESSION:  ${'$'}{{ matrix.expression }}
          INPUT_RIG_ANIM:    ${'$'}{{ matrix.rig_anim }}
          INPUT_QUALITY:     ${'$'}{{ matrix.quality }}
        run: |
          mkdir -p axev-build
          python3 << 'PYEOF'
          import json, os
          def env(k, d=""): return os.environ.get(k, d).strip()
          preset = env("INPUT_PRESET", "Athletic man")
          params = {"name": env("INPUT_NAME", preset) or preset, "detail": env("INPUT_QUALITY", "high").lower()}
          ga = env("INPUT_GENDER_AGE", "male:25").split(":")
          params["gender"] = 0.0 if ga[0] == "female" else (0.5 if ga[0] == "androgynous" else 1.0)
          muscle_map = {"none":0.1,"slim":0.3,"medium":0.5,"athletic":0.7,"muscular":0.85,"very muscular":1.0}
          weight_map = {"very slim":0.15,"slim":0.3,"average":0.5,"heavy":0.7,"very heavy":0.9}
          height_map = {"very short":0.1,"short":0.3,"average":0.5,"tall":0.7,"very tall":0.9}
          bp = env("INPUT_BODY","medium:average:average").split(":")
          if len(bp)>=1: params["muscle"]=muscle_map.get(bp[0],0.5)
          if len(bp)>=2: params["weight"]=weight_map.get(bp[1],0.5)
          if len(bp)>=3: params["height"]=height_map.get(bp[2],0.5)
          skin_map={"african":{"african":1.0,"asian":0.0,"caucasian":0.0},"asian":{"african":0.0,"asian":1.0,"caucasian":0.0},"caucasian":{"african":0.0,"asian":0.0,"caucasian":1.0},"dark":{"african":0.7,"asian":0.1,"caucasian":0.2},"light":{"african":0.0,"asian":0.1,"caucasian":0.9},"mixed":{"african":0.3,"asian":0.2,"caucasian":0.5}}
          params.update(skin_map.get(env("INPUT_SKIN","mixed"),{"african":0.3,"asian":0.2,"caucasian":0.5}))
          hp=env("INPUT_HAIR","short02:black").split(":")
          if hp[0]!="preset_default": params["hair"]=hp[0]
          fh=env("INPUT_FACIAL_HAIR","none")
          if fh!="preset_default": params["facialHair"]=fh
          params["eyes"]=env("INPUT_EYES","brown"); params["expression"]=env("INPUT_EXPRESSION","neutral")
          w=env("INPUT_WEAPON","none")
          if w not in ("preset_default",""): params["weapon"]=w
          outfit=env("INPUT_OUTFIT","preset_default")
          if outfit=="preset_default": pass
          elif "+" in outfit:
            top,bottom=outfit.split("+",1); params["outfit"]="none"; params["top"]=top; params["bottom"]=bottom
          elif outfit=="none": params["outfit"]="none"
          else: params["outfit"]=outfit
          sh=env("INPUT_SHOES_HAT","preset_default:none").split(":")
          if sh[0] not in ("preset_default",""): params["shoes"]=sh[0]
          if len(sh)>1 and sh[1] not in ("preset_default",""): params["hat"]=sh[1]
          ra=env("INPUT_RIG_ANIM","game:yes:A").split(":")
          params["rig"]=ra[0]; params["animations"]=(ra[1] if len(ra)>1 else "yes")=="yes"; params["restPose"]=ra[2] if len(ra)>2 else "A"
          params["faceShapes"]=True; params["speechShapes"]=True; params["faceBones"]=True
          with open("axev-build/character_overrides.json","w") as f: json.dump(params,f,indent=2)
          print(json.dumps(params,indent=2))
          PYEOF
      - name: Gerar personagem ${'$'}{{ matrix.id }}
        working-directory: ara-3d-gen
        env:
          FORMATS_NORM:    ${'$'}{{ env.FORMATS_NORM }}
          RESOLVED_PRESET: ${'$'}{{ matrix.preset }}
        run: |
          mkdir -p out
          cat > merge-params.ts << 'TSEOF'
          import { readFileSync, writeFileSync } from 'node:fs';
          import { DEFAULT_PARAMS, PRESETS } from './src/core/params';
          const presetName = process.env.RESOLVED_PRESET ?? 'Athletic man';
          const preset = (PRESETS as Record<string, object>)[presetName] ?? {};
          const overrides = JSON.parse(readFileSync('../axev-build/character_overrides.json', 'utf8'));
          writeFileSync('../axev-build/character.params.json', JSON.stringify({...DEFAULT_PARAMS,...preset,...overrides},null,2));
          TSEOF
          npx tsx merge-params.ts
          npm run export -- --params "../axev-build/character.params.json" --formats "${'$'}FORMATS_NORM" --out out
      - name: Validar ${'$'}{{ matrix.id }}
        env:
          FORMATS_NORM: ${'$'}{{ env.FORMATS_NORM }}
        run: |
          OUT="ara-3d-gen/out"
          IFS=',' read -ra FMTS <<< "${'$'}FORMATS_NORM"
          for fmt in "${'$'}{FMTS[@]}"; do
            fmt=$(echo "${'$'}fmt"|tr -d '[:space:]')
            FILES=$(find "${'$'}OUT" -name "*.${'$'}{fmt}" -type f 2>/dev/null)
            if [ -z "${'$'}FILES" ]; then echo "::error::[${'$'}{{ matrix.id }}] Formato ${'$'}fmt não produzido."; exit 1; fi
            echo "✅ [${'$'}{{ matrix.id }}] ${'$'}fmt OK"
          done
      - name: Publicar artefacto ${'$'}{{ matrix.id }}
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: smoke-${'$'}{{ matrix.id }}-${'$'}{{ matrix.character_id }}
          path: ara-3d-gen/out/
          if-no-files-found: warn
          retention-days: 3

  report:
    name: "Relatório do Smoke Test"
    runs-on: ubuntu-latest
    needs: smoke
    if: always()
    steps:
      - name: Resumo
        run: |
          echo "## 🧪 AXEV Smoke Test" >> ${'$'}GITHUB_STEP_SUMMARY
          echo "| TC01 | TC02 | TC03 | TC04 | TC05 | TC06 |" >> ${'$'}GITHUB_STEP_SUMMARY
          echo "|------|------|------|------|------|------|" >> ${'$'}GITHUB_STEP_SUMMARY
          echo "| Guerreiro Africano | Executiva Asiática | Andrógino | Elder | Agente+Arma | Jovem Feminina |" >> ${'$'}GITHUB_STEP_SUMMARY
""".trimIndent()

    // ── Funções públicas ──────────────────────────────────────────────────────

    /** Conteúdo do workflow de geração em Base64 (para GitHub API) */
    fun generateWorkflowBase64(): String =
        Base64.encodeToString(GENERATE_CONTENT.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    /** Conteúdo do workflow de smoke test em Base64 (para GitHub API) */
    fun smokeWorkflowBase64(): String =
        Base64.encodeToString(SMOKE_CONTENT.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
}
