import { Delaunay } from "d3-delaunay"
import polygonClipping from "polygon-clipping"

/** 自贡市行政区划（与 public/geo/zigong-city.json 一致） */
export const ZIGONG_DISTRICT_ADCODE: Record<string, string> = {
  自流井区: "510302",
  贡井区: "510303",
  大安区: "510304",
  沿滩区: "510311",
  荣县: "510321",
  富顺县: "510322"
}

export interface GeoJsonFeatureCollection {
  type: "FeatureCollection"
  features: GeoJsonFeature[]
}

export interface GeoJsonFeature {
  type: "Feature"
  properties: { name: string, adcode?: number | string }
  geometry: {
    type: string
    coordinates: number[][][] | number[][][][]
  }
}

type Position = [number, number]
type Ring = Position[]
type Polygon = Ring[]
type MultiPolygon = Polygon[]

let cityGeoCache: GeoJsonFeatureCollection | null = null

export async function loadZigongCityGeo(): Promise<GeoJsonFeatureCollection> {
  if (cityGeoCache) return cityGeoCache
  const res = await fetch(`${import.meta.env.BASE_URL}geo/zigong-city.json`)
  if (!res.ok) {
    throw new Error("加载自贡地图失败")
  }
  cityGeoCache = await res.json()
  return cityGeoCache!
}

export function normalizeDistrictName(name: string) {
  return name.trim().replace(/^自贡市/, "").replace(/\s+/g, "")
}

function stripDistrictAdminSuffix(name: string) {
  if (name.endsWith("区") || name.endsWith("县")) {
    return name.slice(0, -1)
  }
  return name
}

export function districtNamesMatch(a: string, b: string) {
  const na = normalizeDistrictName(a)
  const nb = normalizeDistrictName(b)
  if (!na || !nb) return false
  if (na === nb) return true
  return stripDistrictAdminSuffix(na) === stripDistrictAdminSuffix(nb)
}

function normalizeTownshipName(name: string) {
  return name.trim()
    .replace(/(彝族|苗族|回族|藏族|蒙古族|土家族|侗族|布依族|哈尼族|傣族|白族|傈僳族|佤族|拉祜族|纳西族|景颇族|柯尔克孜族|土族|达斡尔族|仫佬族|羌族|布朗族|撒拉族|毛南族|仡佬族|锡伯族|阿昌族|普米族|塔吉克族|怒族|乌孜别克族|俄罗斯族|鄂温克族|德昂族|保安族|裕固族|京族|塔塔尔族|独龙族|鄂伦春族|赫哲族|门巴族|珞巴族|基诺族)/g, "")
    .replace(/(街道办事处|街道办|街道|[镇乡村]|社区)$/g, "")
}

export function townshipNamesMatch(a: string, b: string) {
  const na = normalizeTownshipName(a)
  const nb = normalizeTownshipName(b)
  if (!na || !nb) return false
  if (na === nb) return true
  return na.includes(nb) || nb.includes(na)
}

export function findDistrictFeature(geo: GeoJsonFeatureCollection, districtName: string) {
  return geo.features.find(f => districtNamesMatch(f.properties.name, districtName))
}

/** 加载区县下乡镇 GeoJSON：优先本地边界文件，否则在区县真实轮廓内生成 Voronoi 乡镇面 */
export async function loadDistrictTownshipGeo(
  districtFeature: GeoJsonFeature,
  districtAdcode: string | undefined,
  regions: Array<{ name: string, value?: number }>
): Promise<GeoJsonFeatureCollection> {
  const adcode = districtAdcode || String(districtFeature.properties.adcode ?? "")
  if (adcode) {
    try {
      const res = await fetch(`${import.meta.env.BASE_URL}geo/townships/${adcode}.json`)
      if (res.ok) {
        const bundled = await res.json() as GeoJsonFeatureCollection
        if (bundled?.features?.length) {
          return ensureRegionCoverage(bundled, districtFeature, regions)
        }
      }
    } catch {
      // 使用 Voronoi 兜底
    }
  }
  return buildTownshipVoronoiGeoJson(districtFeature, regions)
}

function ensureRegionCoverage(
  bundled: GeoJsonFeatureCollection,
  districtFeature: GeoJsonFeature,
  regions: Array<{ name: string, value?: number }>
) {
  const missing = (regions ?? []).filter(region =>
    !bundled.features.some(feature => townshipNamesMatch(feature.properties.name, region.name))
  )
  if (!missing.length) {
    return bundled
  }
  const generated = buildTownshipVoronoiGeoJson(districtFeature, missing)
  return {
    type: "FeatureCollection" as const,
    features: [...bundled.features, ...generated.features]
  }
}

/** 在区县真实边界内按 Voronoi 划分乡镇面（替代矩形网格） */
export function buildTownshipVoronoiGeoJson(
  districtFeature: GeoJsonFeature,
  regions: Array<{ name: string, value?: number }>
): GeoJsonFeatureCollection {
  const labels = (regions ?? [])
    .map(region => region.name?.trim())
    .filter((name): name is string => !!name && name !== "—")

  if (!labels.length) {
    return { type: "FeatureCollection", features: [] }
  }

  const districtPolygons = featureToMultiPolygon(districtFeature)
  const { minX, minY, maxX, maxY } = getFeatureBBox(districtFeature)
  const seeds = labels.map((name, index) => ({
    name,
    point: samplePointInMultiPolygon(districtPolygons, hashSeed(name, index), minX, minY, maxX, maxY)
  }))

  const delaunay = Delaunay.from(seeds, d => d.point[0], d => d.point[1])
  const voronoi = delaunay.voronoi([minX, minY, maxX, maxY])
  const features: GeoJsonFeature[] = []

  for (let i = 0; i < seeds.length; i++) {
    const cell = voronoi.cellPolygon(i)
    if (!cell || cell.length < 4) continue

    const clipped = clipPolygonToDistrict(cell as Position[], districtPolygons)
    if (!clipped.length) continue

    features.push({
      type: "Feature",
      properties: { name: seeds[i].name },
      geometry: {
        type: clipped.length > 1 ? "MultiPolygon" : "Polygon",
        coordinates: clipped.length > 1
          ? clipped.map(ring => [ring])
          : clipped
      }
    })
  }

  return { type: "FeatureCollection", features }
}

/** @deprecated 使用 loadDistrictTownshipGeo / buildTownshipVoronoiGeoJson */
export function buildTownshipGeoJson(
  districtFeature: GeoJsonFeature,
  regions: Array<{ name: string, value?: number }>
): GeoJsonFeatureCollection {
  return buildTownshipVoronoiGeoJson(districtFeature, regions)
}

function featureToMultiPolygon(feature: GeoJsonFeature): MultiPolygon {
  if (feature.geometry.type === "MultiPolygon") {
    return (feature.geometry.coordinates as number[][][][]).map(polygon =>
      polygon.map(ring => ring.map(([x, y]) => [x, y] as Position))
    )
  }
  const polygon = (feature.geometry.coordinates as number[][][]).map(ring =>
    ring.map(([x, y]) => [x, y] as Position)
  )
  return [polygon]
}

function multiPolygonToClippingInput(polygons: MultiPolygon) {
  return polygons as polygonClipping.Geom
}

function clipPolygonToDistrict(cell: Position[], districtPolygons: MultiPolygon): Ring[] {
  const districtInput = multiPolygonToClippingInput(districtPolygons)
  const cellInput: polygonClipping.Geom = [[cell]]
  const result = polygonClipping.intersection(districtInput, cellInput) as Ring[][] | null
  if (!result?.length) return []

  return result.map(polygon => polygon[0])
}

function hashSeed(name: string, index: number) {
  let hash = index + 1
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) >>> 0
  }
  return hash
}

function samplePointInMultiPolygon(
  polygons: MultiPolygon,
  seed: number,
  minX: number,
  minY: number,
  maxX: number,
  maxY: number
): Position {
  let state = seed || 1
  for (let attempt = 0; attempt < 5000; attempt++) {
    state = (state * 1664525 + 1013904223) >>> 0
    const x = minX + (state / 0xFFFFFFFF) * (maxX - minX)
    state = (state * 1664525 + 1013904223) >>> 0
    const y = minY + (state / 0xFFFFFFFF) * (maxY - minY)
    if (pointInMultiPolygon([x, y], polygons)) {
      return [x, y]
    }
  }
  return [(minX + maxX) / 2, (minY + maxY) / 2]
}

function pointInMultiPolygon(point: Position, polygons: MultiPolygon) {
  return polygons.some(polygon => pointInPolygon(point, polygon[0] ?? []))
}

function pointInPolygon(point: Position, ring: Ring) {
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

function getFeatureBBox(feature: GeoJsonFeature) {
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity

  const polygons = feature.geometry.type === "MultiPolygon"
    ? feature.geometry.coordinates as number[][][][]
    : [feature.geometry.coordinates as number[][][]]

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
  return {
    minX: minX - padX,
    minY: minY - padY,
    maxX: maxX + padX,
    maxY: maxY + padY
  }
}

export function buildMapSeriesData(
  regions: Array<{ name: string, value?: number }> | undefined,
  geoFeatures: GeoJsonFeature[]
) {
  const used = new Set<string>()
  return geoFeatures.map((feature) => {
    const name = feature.properties.name
    const matched = (regions ?? []).find((region) => {
      if (used.has(region.name)) return false
      if (townshipNamesMatch(region.name, name)) {
        used.add(region.name)
        return true
      }
      return false
    })
    return {
      name,
      value: matched?.value ?? 0
    }
  })
}
