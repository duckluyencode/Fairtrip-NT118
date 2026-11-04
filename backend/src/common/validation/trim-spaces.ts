import { Transform } from 'class-transformer';

/** Trims and collapses inner spaces, e.g. "  Duy   Đức " -> "Duy Đức". */
export const TrimSpaces = () =>
  Transform(({ value }: { value: unknown }) =>
    typeof value === 'string' ? value.trim().replace(/\s+/g, ' ') : value,
  );
