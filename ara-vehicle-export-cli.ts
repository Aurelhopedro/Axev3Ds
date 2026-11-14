/**
 * Headless vehicle export — gera o veículo 3D e (opcionalmente) o rider.
 *
 * Uso:
 *   npx tsx ara-vehicle-export-cli.ts \
 *     --vehicle sports_car \
 *     --color "#c0392b" --finish gloss --variant standard \
 *     --rider-params rider.params.json \
 *     --with-rider yes \
 *     --formats glb \
 *     --out out/vehicles
 */
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { buildHuman } from './src/core/human';
import { DEFAULT_PARAMS } from './src/core/params';
import { Animator } from './src/core/anim/evaluate';
import { clipById } from './src/core/anim/clips';
import { Vehicle } from './src/core/props/vehicle';
import { VEHICLE_CLIP, DEFAULT_VEHICLE_STYLE } from './src/core/props/vehicles';
import { exportGLB } from './src/export/glb';
import { exportFBX } from './src/export/fbx';
import { exportCharacter } from './src/export';
import { nodeLibrary } from './scripts/node-assets';

// ── helpers ──────────────────────────────────────────────────────────────────
const args = process.argv.slice(2);
const opt = (k: string) => { const i = args.indexOf(`--${k}`); return i >= 0 ? args[i + 1] : undefined; };

const vehicleId   = (opt('vehicle') ?? 'sports_car') as string;
const withRider   = (opt('with-rider') ?? 'yes') === 'yes';
const formatsRaw  = (opt('formats') ?? 'glb').split(',');
const outDir      = opt('out') ?? 'out/vehicles';
const riderParams = opt('rider-params');
const colorHex    = opt('color') ?? '#c0392b';
const finish      = (opt('finish') ?? 'gloss') as 'gloss' | 'matte';
const variant     = opt('variant') ?? 'standard';

mkdirSync(outDir, { recursive: true });

// ── estilo do veículo ─────────────────────────────────────────────────────────
const vehicleStyle = {
  ...DEFAULT_VEHICLE_STYLE,
  color: colorHex,
  finish,
  ...(variant !== 'standard' ? { variant } : {}),
};

// ── carregar motor ────────────────────────────────────────────────────────────
const lib = await nodeLibrary();

// ── rider ─────────────────────────────────────────────────────────────────────
const riderOverrides = riderParams
  ? JSON.parse(readFileSync(riderParams, 'utf8'))
  : {};
const riderParamsFull = { ...DEFAULT_PARAMS, detail: 'high', rig: 'game', animations: true, ...riderOverrides };
const ch = await buildHuman(lib, riderParamsFull);

// ── animação de condução ──────────────────────────────────────────────────────
const clipId  = VEHICLE_CLIP[vehicleId as keyof typeof VEHICLE_CLIP];
if (!clipId) {
  console.error(`Veículo desconhecido: "${vehicleId}". Disponíveis: ${Object.keys(VEHICLE_CLIP).join(', ')}`);
  process.exit(1);
}
const clip = clipById.get(clipId);
if (!clip) {
  console.error(`Clip não encontrado: "${clipId}"`);
  process.exit(1);
}

const animator = new Animator(ch.rig!);

// ── gerar veículo ─────────────────────────────────────────────────────────────
const vehicle = new Vehicle(vehicleId as any, ch.rig!, animator, clip, vehicleStyle);
const { mesh: vehicleMesh, rig: vehicleRig, bake } = vehicle.asset();
const baked = bake();

const writtenFiles: string[] = [];

for (const fmt of formatsRaw) {
  const fmtClean = fmt.trim().toLowerCase();

  if (fmtClean === 'glb' || fmtClean === 'fbx') {
    // ── veículo ──
    const vehicleData = fmtClean === 'glb'
      ? exportGLB(vehicleMesh, vehicleId, new Map(), vehicleRig, [baked])
      : exportFBX(vehicleMesh, vehicleId, new Map(), vehicleRig, [baked]);

    const buffer = Buffer.from(vehicleData.buffer, vehicleData.byteOffset, vehicleData.byteLength);
    const vehicleFile = join(outDir, `${vehicleId}.${fmtClean}`);
    writeFileSync(vehicleFile, buffer);
    const kb = Math.round(buffer.length / 1024);
    console.log(`✅ Veículo: ${vehicleFile} (${kb} KB)`);
    writtenFiles.push(vehicleFile);

    // ── rider ──
    if (withRider) {
      const riderFile = await exportCharacter(lib, ch, fmtClean as any);
      const riderPath = join(outDir, riderFile.name);
      writeFileSync(riderPath, riderFile.data);
      const rkb = Math.round(riderFile.data.length / 1024);
      console.log(`✅ Rider:   ${riderPath} (${rkb} KB)`);
      writtenFiles.push(riderPath);
    }
  } else {
    console.warn(`⚠️  Formato não suportado para veículos: ${fmtClean} (use glb ou fbx)`);
  }
}

vehicle.dispose();

console.log('');
console.log(`📦 Total: ${writtenFiles.length} ficheiro(s) gerado(s) em ${outDir}`);
