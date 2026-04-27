/**
 * Build a browser-usable URL for playlist/layout media.
 * - Absolute http(s) URLs (S3, CDN, full site URLs) are returned as-is.
 * - Relative paths (/uploads/...) are prefixed with publicBaseUrl (API origin without /api).
 */
export function resolvePublicMediaUrl(
  mediaUrl: string | undefined | null,
  publicBaseUrl: string
): string | null {
  if (mediaUrl == null || String(mediaUrl).trim() === '') return null;
  const u = String(mediaUrl).trim();
  if (/^https?:\/\//i.test(u)) return u;
  const base = publicBaseUrl.replace(/\/$/, '');
  const path = u.startsWith('/') ? u : `/${u}`;
  return `${base}${path}`;
}
