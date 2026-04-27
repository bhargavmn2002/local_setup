const fs = require('fs');
const path = require('path');
const { PutObjectCommand, DeleteObjectCommand } = require('@aws-sdk/client-s3');
const s3 = require('../config/s3');

/** Accepts true, 1, yes, on (any case); rejects unset/false/0. */
function envFlagEnabled(name) {
  const v = process.env[name];
  if (v == null || String(v).trim() === '') return false;
  const s = String(v).trim().toLowerCase();
  return s === 'true' || s === '1' || s === 'yes' || s === 'on';
}

function isS3MediaEnabled() {
  return envFlagEnabled('USE_S3_FOR_MEDIA') && !!String(process.env.AWS_S3_BUCKET || '').trim();
}

function getPrefix() {
  return (process.env.AWS_S3_MEDIA_PREFIX || 'media').replace(/^\/+|\/+$/g, '');
}

/**
 * Public URL for an object key. Use AWS_S3_PUBLIC_BASE_URL for CloudFront or custom domain.
 */
function buildPublicUrl(key) {
  const k = String(key || '').replace(/^\//, '');
  if (!k) return '';
  const custom = process.env.AWS_S3_PUBLIC_BASE_URL;
  if (custom) {
    const base = custom.replace(/\/$/, '');
    return `${base}/${k}`;
  }
  const bucket = process.env.AWS_S3_BUCKET;
  const region = process.env.AWS_REGION || 'ap-south-1';
  return `https://${bucket}.s3.${region}.amazonaws.com/${k}`;
}

async function uploadBuffer(key, buffer, contentType) {
  await s3.send(
    new PutObjectCommand({
      Bucket: process.env.AWS_S3_BUCKET,
      Key: key,
      Body: buffer,
      ContentType: contentType || 'application/octet-stream',
    })
  );
  return buildPublicUrl(key);
}

async function uploadFile(key, filePath, contentType) {
  const buffer = fs.readFileSync(filePath);
  return uploadBuffer(key, buffer, contentType);
}

async function deleteKeys(keys) {
  const bucket = process.env.AWS_S3_BUCKET;
  if (!bucket) return;
  const unique = [...new Set((keys || []).filter(Boolean))];
  for (const key of unique) {
    try {
      await s3.send(
        new DeleteObjectCommand({
          Bucket: bucket,
          Key: key,
        })
      );
    } catch (e) {
      console.warn('S3 DeleteObject failed:', key, e.message);
    }
  }
}

function keysFromMediaRecord(media) {
  const keys = [];
  if (media.s3Key) keys.push(media.s3Key);
  if (Array.isArray(media.s3ExtraKeys)) keys.push(...media.s3ExtraKeys);
  return [...new Set(keys.filter(Boolean))];
}

/**
 * Best-effort key extraction from a full S3 virtual-hosted URL (not CloudFront).
 */
function tryExtractKeysFromUrl(url) {
  if (!url || !/^https?:\/\//i.test(url)) return [];
  try {
    const u = new URL(url);
    const bucket = process.env.AWS_S3_BUCKET;
    const region = process.env.AWS_REGION || 'ap-south-1';
    if (!bucket) return [];
    const hostOk =
      u.hostname === `${bucket}.s3.${region}.amazonaws.com` ||
      u.hostname === `${bucket}.s3.amazonaws.com` ||
      u.hostname === `s3.${region}.amazonaws.com`;
    if (!hostOk) return [];
    const key = decodeURIComponent(u.pathname.replace(/^\//, ''));
    return key ? [key] : [];
  } catch {
    return [];
  }
}

function posixDirname(key) {
  const p = String(key).replace(/\\/g, '/');
  const i = p.lastIndexOf('/');
  return i === -1 ? '' : p.slice(0, i);
}

function posixBasenameNoExt(key) {
  const base = path.posix.basename(String(key).replace(/\\/g, '/'));
  return path.parse(base).name;
}

function thumbnailKeyFromMainKey(mainKey) {
  const dir = posixDirname(mainKey);
  const base = posixBasenameNoExt(mainKey);
  const prefix = dir ? `${dir}/` : '';
  return `${prefix}thumbnails/${base}_thumb.jpg`;
}

function previewKeyFromMainKey(mainKey) {
  const dir = posixDirname(mainKey);
  const base = posixBasenameNoExt(mainKey);
  const prefix = dir ? `${dir}/` : '';
  return `${prefix}optimized/${base}_preview.jpg`;
}

module.exports = {
  isS3MediaEnabled,
  getPrefix,
  buildPublicUrl,
  uploadBuffer,
  uploadFile,
  deleteKeys,
  keysFromMediaRecord,
  tryExtractKeysFromUrl,
  thumbnailKeyFromMainKey,
  previewKeyFromMainKey,
};
