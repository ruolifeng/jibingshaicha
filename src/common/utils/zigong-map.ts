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

export function findDistrictFeature(geo: GeoJsonFeatureCollection, districtName: string) {
  return geo.features.find(f => districtNamesMatch(f.properties.name, districtName))
}

/** 在区县边界包围盒内按网格生成乡镇级 GeoJSON（用于下钻展示） */
export function buildTownshipGeoJson(
  districtFeature: GeoJsonFeature,
  regions: Array<{ name: string, value?: number }>
): GeoJsonFeatureCollection {
  const { minX, minY, maxX, maxY } = getFeatureBBox(districtFeature)
  const width = maxX - minX
  const height = maxY - minY
  const count = Math.max(regions.length, 1)
  const cols = Math.ceil(Math.sqrt(count))
  const rows = Math.ceil(count / cols)
  const cellW = width / cols
  const cellH = height / rows

  const features: GeoJsonFeature[] = regions.map((region, index) => {
    const row = Math.floor(index / cols)
    const col = index % cols
    const x0 = minX + col * cellW
    const y0 = minY + row * cellH
    const x1 = x0 + cellW
    const y1 = y0 + cellH
    return {
      type: "Feature",
      properties: { name: region.name },
      geometry: {
        type: "Polygon",
        coordinates: [[
          [x0, y0],
          [x1, y0],
          [x1, y1],
          [x0, y1],
          [x0, y0]
        ]]
      }
    }
  })

  return { type: "FeatureCollection", features }
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
  return geoFeatures.map((feature) => {
    const name = feature.properties.name
    const matched = (regions ?? []).find(r =>
      r.name === name || districtNamesMatch(r.name, name)
    )
    return {
      name,
      value: matched?.value ?? 0
    }
  })
}
