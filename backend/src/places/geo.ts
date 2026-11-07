export interface LatLng {
  lat: number;
  lng: number;
}

const EARTH_RADIUS_KM = 6371;

/** Great-circle distance in km, rounded to 0.1 km. */
export function distanceKm(from: LatLng, to: LatLng): number {
  const rad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = rad(to.lat - from.lat);
  const dLng = rad(to.lng - from.lng);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(rad(from.lat)) * Math.cos(rad(to.lat)) * Math.sin(dLng / 2) ** 2;
  const km = 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
  return Math.round(km * 10) / 10;
}
