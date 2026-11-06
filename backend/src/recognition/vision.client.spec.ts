import { ConfigService } from '@nestjs/config';
import { isSupportedImage } from './recognition.service.js';
import { HttpVisionClient } from './vision.client.js';

const JPEG = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10]);

function client(apiKey?: string) {
  return new HttpVisionClient(
    new ConfigService(apiKey ? { GOOGLE_MAPS_API_KEY: apiKey } : {}),
  );
}

describe('isSupportedImage', () => {
  it('recognizes JPEG, PNG and WebP by their first bytes', () => {
    expect(isSupportedImage(JPEG)).toBe(true);
    expect(
      isSupportedImage(
        Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0]),
      ),
    ).toBe(true);
    expect(isSupportedImage(Buffer.from('RIFF\0\0\0\0WEBPVP8 '))).toBe(true);
    expect(isSupportedImage(Buffer.from('GIF89a'))).toBe(false);
    expect(isSupportedImage(Buffer.alloc(0))).toBe(false);
  });
});

describe('HttpVisionClient', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('is disabled without an API key', async () => {
    const vision = client();
    expect(vision.enabled).toBe(false);
    await expect(vision.landmarks(JPEG)).rejects.toMatchObject({
      code: 'RECOGNITION_UNAVAILABLE',
    });
  });

  it('sends the photo as base64 for landmark detection and maps the answer', async () => {
    const fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          responses: [
            {
              landmarkAnnotations: [
                { description: 'Marble Mountains', score: 0.4 },
                {
                  description: 'Dragon Bridge',
                  score: 0.9,
                  locations: [
                    { latLng: { latitude: 16.0611, longitude: 108.2271 } },
                  ],
                },
              ],
            },
          ],
        }),
        { status: 200 },
      ),
    );
    vi.stubGlobal('fetch', fetch);

    const landmarks = await client('test-key').landmarks(JPEG);

    expect(landmarks).toEqual([
      {
        name: 'Dragon Bridge',
        score: 0.9,
        location: { lat: 16.0611, lng: 108.2271 },
      },
      { name: 'Marble Mountains', score: 0.4, location: null },
    ]);
    const [url, init] = fetch.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('https://vision.googleapis.com/v1/images:annotate');
    expect(init.headers).toMatchObject({ 'X-Goog-Api-Key': 'test-key' });
    expect(JSON.parse(init.body as string)).toEqual({
      requests: [
        {
          image: { content: JPEG.toString('base64') },
          features: [{ type: 'LANDMARK_DETECTION', maxResults: 5 }],
        },
      ],
    });
  });

  it('reports 503 when Vision fails or cannot read the image', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(new Response('denied', { status: 403 }))
        .mockResolvedValueOnce(
          new Response(
            JSON.stringify({
              responses: [{ error: { code: 3, message: 'Bad image data.' } }],
            }),
            { status: 200 },
          ),
        ),
    );
    const vision = client('test-key');
    await expect(vision.landmarks(JPEG)).rejects.toMatchObject({
      code: 'RECOGNITION_UNAVAILABLE',
    });
    await expect(vision.landmarks(JPEG)).rejects.toMatchObject({
      code: 'RECOGNITION_UNAVAILABLE',
    });
  });
});
