import { PlaceSummary } from '../places/dto/place.response.js';

export class RecognizedPlace {
  /** What Cloud Vision saw, e.g. "Dragon Bridge". */
  landmark: string;
  /** 0–1. */
  confidence: number;
  /** The matching place (no price, like every list); null when none was found. */
  place: PlaceSummary | null;
}

export class RecognitionResponse {
  /** Most likely first; empty when no landmark was recognized. */
  items: RecognizedPlace[];
  attribution: string | null;
}
