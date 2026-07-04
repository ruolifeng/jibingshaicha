/**
 * 预生成自贡各区县乡镇 GeoJSON（区县真实轮廓 + 乡镇中心点 Voronoi）
 * 乡镇名录：admin/src/main/resources/geo/zigong-townships.json
 * 中心坐标：public/geo/township-centroids.json
 * 运行：node admin/scripts/generate-township-geo.mjs
 */
import { mkdirSync, readFileSync, writeFileSync } from "node:fs"
import { dirname, join } from "node:path"
import { fileURLToPath } from "node:url"
import { Delaunay } from "d3-delaunay"
import polygonClipping from "polygon-clipping"

const __dirname = dirname(fileURLToPath(import.meta.url))
const rootDir = join(__dirname, "..", "..")
const cityGeoPath = join(rootDir, "public", "geo", "zigong-city.json")
const catalogPath = join(rootDir, "admin", "src", "main", "resources", "geo", "zigong-townships.json")
const centroidsPath = join(rootDir, "public", "geo", "township-centroids.json")
const outputDir = join(rootDir, "public", "geo", "townships")

const DISTRICT_ADCODES = {
  自流井区: "510302",
  贡井区: "510303",
  大安区: "510304",
  沿滩区: "510311",
  荣县: "510321",
  富顺县: "510322"
}

const cityGeo = JSON.parse(readFileSync(cityGeoPath, "utf8"))
const catalog = JSON.parse(readFileSync(catalogPath, "utf8"))
const centroids = JSON.parse(readFileSync(centroidsPath, "utf8"))

mkdirSync(outputDir, { recursive: true })

for (const [districtName, adcode] of Object.entries(DISTRICT_ADCODES)) {
  const districtFeature = cityGeo.features.find(f => String(f.properties.adcode) === adcode)
  if (!districtFeature) {
    console.warn(`skip ${districtName}: feature not found`)
    continue
  }

  const labels = catalog[districtName] ?? []
  if (!labels.length) {
    console.warn(`skip ${districtName}: empty catalog`)
    continue
  }

  const geo = buildTownshipVoronoiGeoJson(
    districtFeature,
    labels.map(name => ({ name })),
    centroids[districtName] ?? {}
  )
  const outPath = join(outputDir, `${adcode}.json`)
  writeFileSync(outPath, `${JSON.stringify(geo)}\n`, "utf8")
  console.log(`generated ${outPath} (${geo.features.length} features)`)
}

function buildTownshipVoronoiGeoJson(districtFeature, regions, districtCentroids) {
  const labels = regions.map(r => r.name).filter(Boolean)
  const districtPolygons = featureToMultiPolygon(districtFeature)
  const { minX, minY, maxX, maxY } = getFeatureBBox(districtFeature)
  const seeds = labels.map((name, index) => ({
    name,
    point: resolveSeedPoint(name, index, districtCentroids, districtPolygons, minX, minY, maxX, maxY)
  }))

  const delaunay = Delaunay.from(seeds, d => d.point[0], d => d.point[1])
  const voronoi = delaunay.voronoi([minX, minY, maxX, maxY])
  const features = []

  for (let i = 0; i < seeds.length; i++) {
    const cell = voronoi.cellPolygon(i)
    if (!cell || cell.length < 4) continue
    const clipped = clipPolygonToDistrict(cell, districtPolygons)
    if (!clipped.length) continue
    features.push({
      type: "Feature",
      properties: { name: seeds[i].name },
      geometry: {
        type: clipped.length > 1 ? "MultiPolygon" : "Polygon",
        coordinates: clipped.length > 1
          ? clipped.map(polygon => [polygon])
          : [clipped[0]]
      }
    })
  }

  return { type: "FeatureCollection", features }
}

function resolveSeedPoint(name, index, districtCentroids, districtPolygons, minX, minY, maxX, maxY) {
  const preset = districtCentroids?.[name]
  if (preset?.length === 2) {
    const point = [preset[0], preset[1]]
    if (pointInMultiPolygon(point, districtPolygons)) {
      return point
    }
  }
  return samplePointInMultiPolygon(districtPolygons, hashSeed(name, index), minX, minY, maxX, maxY)
}

function featureToMultiPolygon(feature) {
  if (feature.geometry.type === "MultiPolygon") {
    return feature.geometry.coordinates.map(polygon =>
      polygon.map(ring => ring.map(([x, y]) => [x, y]))
    )
  }
  return [feature.geometry.coordinates.map(ring => ring.map(([x, y]) => [x, y]))]
}

function multiPolygonToClippingInput(polygons) {
  return polygons.map(polygon => polygon.map(ring => ring.map(([x, y]) => [x, y])))
}

function clipPolygonToDistrict(cell, districtPolygons) {
  const result = polygonClipping.intersection(
    multiPolygonToClippingInput(districtPolygons),
    [[cell.map(([x, y]) => [x, y])]]
  )
  if (!result?.length) return []
  return result.map(polygon => polygon[0].map(([x, y]) => [x, y]))
}

function hashSeed(name, index) {
  let hash = index + 1
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) >>> 0
  return hash
}

function samplePointInMultiPolygon(polygons, seed, minX, minY, maxX, maxY) {
  let state = seed || 1
  for (let attempt = 0; attempt < 5000; attempt++) {
    state = (state * 1664525 + 1013904223) >>> 0
    const x = minX + (state / 0xFFFFFFFF) * (maxX - minX)
    state = (state * 1664525 + 1013904223) >>> 0
    const y = minY + (state / 0xFFFFFFFF) * (maxY - minY)
    if (pointInMultiPolygon([x, y], polygons)) return [x, y]
  }
  return [(minX + maxX) / 2, (minY + maxY) / 2]
}

function pointInMultiPolygon(point, polygons) {
  return polygons.some(polygon => pointInPolygon(point, polygon[0]))
}

function pointInPolygon(point, ring) {
  const [x, y] = point
  let inside = false
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i]
    const [xj, yj] = ring[j]
    const intersect = ((yi > y) !== (yj > y))
      && (x < ((xj - xi) * (y - yi)) / ((yj - yi) || 1e-12) + xi)
    if (intersect) inside = !inside
  }
  return inside
}

function getFeatureBBox(feature) {
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  const polygons = feature.geometry.type === "MultiPolygon"
    ? feature.geometry.coordinates
    : [feature.geometry.coordinates]
  for (const polygon of polygons) {
    for (const ring of polygon) {
      for (const [x, y] of ring) {
        minX = Math.min(minX, x)
        minY = Math.min(minY, y)
        maxX = Math.max(maxX, x)
        maxY = Math.max(maxY, y)
      }
    }
  }
  const padX = (maxX - minX) * 0.02
  const padY = (maxY - minY) * 0.02
  return { minX: minX - padX, minY: minY - padY, maxX: maxX + padX, maxY: maxY + padY }
}
