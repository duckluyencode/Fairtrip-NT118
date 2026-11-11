import { ConfigService } from '@nestjs/config';
import { HttpRoutesClient } from './routes.client.js';

const HOTEL = { lat: 16.05, lng: 108.24 };
const BEACH = { lat: 16.06, lng: 108.25 };
const BRIDGE = { lat: 16.061, lng: 108.227 };

function client(apiKey?: string) {
  return new HttpRoutesClient(
    new ConfigService(apiKey ? { GOOGLE_MAPS_API_KEY: apiKey } : {}),
  );
}

function legsResponse(seconds: number[]) {
  return new Response(
    JSON.stringify({
      routes: [{ legs: seconds.map((s) => ({ duration: `${s}s` })) }],
    }),
    { status: 200 },
  );
}

describe('HttpRoutesClient', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('is disabled without an API key', async () => {
    const routes = client();
    expect(routes.enabled).toBe(false);
    await expect(routes.legMinutes([HOTEL, BEACH])).resolves.toBeNull();
  });

  it('asks for a whole day in one request and returns minutes per leg', async () => {
    const fetch = vi.fn().mockResolvedValue(legsResponse([600, 61, 900]));
    vi.stubGlobal('fetch', fetch);

    const legs = await client('test-key').legMinutes([
      HOTEL,
      BEACH,
      BRIDGE,
      HOTEL,
    ]);

    expect(legs).toEqual([10, 2, 15]);
    const [url, init] = fetch.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      'https://routes.googleapis.com/directions/v2:computeRoutes',
    );
    expect(init.headers).toMatchObject({
      'X-Goog-Api-Key': 'test-key',
      'X-Goog-FieldMask': 'routes.legs.duration',
    });
    expect(JSON.parse(init.body as string)).toMatchObject({
      origin: { location: { latLng: { latitude: 16.05, longitude: 108.24 } } },
      destination: {
        location: { latLng: { latitude: 16.05, longitude: 108.24 } },
      },
      intermediates: [
        { location: { latLng: { latitude: 16.06, longitude: 108.25 } } },
        { location: { latLng: { latitude: 16.061, longitude: 108.227 } } },
      ],
      travelMode: 'DRIVE',
    });
  });

  it('splits days with more than 10 stops between the ends', async () => {
    const points = Array.from({ length: 14 }, (_, i) => ({
      lat: 16 + i / 100,
      lng: 108,
    }));
    const fetch = vi
      .fn()
      .mockImplementation((_url: string, init: RequestInit) => {
        const body = JSON.parse(init.body as string) as {
          intermediates: unknown[];
        };
        return Promise.resolve(
          legsResponse(Array(body.intermediates.length + 1).fill(60)),
        );
      });
    vi.stubGlobal('fetch', fetch);

    const legs = await client('test-key').legMinutes(points);

    expect(legs).toHaveLength(13);
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it('returns null when Google cannot route, so callers keep their estimate', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(new Response('quota', { status: 429 }))
        .mockResolvedValueOnce(legsResponse([600])),
    );
    const routes = client('test-key');
    await expect(routes.legMinutes([HOTEL, BEACH])).resolves.toBeNull();
    // One leg for three points is not a usable answer.
    await expect(routes.legMinutes([HOTEL, BEACH, BRIDGE])).resolves.toBeNull();
  });
});
