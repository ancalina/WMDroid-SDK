import { createHash } from 'node:crypto';
import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

// Complete contract records: change presentation, never shorten conditions or code.
export function compactContract(body) {
  const lines = body.replace(/\r\n/g, '\n').split('\n');
  const output = [];
  let paragraph = [], fenced = false;
  const flush = () => { if (paragraph.length) output.push(paragraph.join(' ')); paragraph = []; };
  for (const line of lines) {
    if (/^\s*```/.test(line)) { flush(); output.push(line); fenced = !fenced; continue; }
    if (fenced) { output.push(line); continue; }
    if (/^\s*<\/?div(?:\s[^>]*)?>\s*$/.test(line)) { flush(); continue; }
    if (!line.trim()) { flush(); if (output.at(-1) !== '') output.push(''); continue; }
    if (/^\s*(?:#{1,6}\s|\||[-*]\s|\d+\.\s|>)/.test(line)) { flush(); output.push(line.trim()); }
    else paragraph.push(line.trim());
  }
  flush();
  if (fenced) throw new Error('Unclosed documentation code block');
  const result = output.join('\n').trim() + '\n';
  const tokens = value => value.replace(/^\s*<\/?div(?:\s[^>]*)?>\s*$/gm, '').replace(/\s/g, '');
  if (tokens(body) !== tokens(result)) throw new Error('Contract content changed during compaction');
  return result;
}

function localUrl(url, base) {
  const prefix = base.replace(/\/$/, '');
  if (!prefix || !url.startsWith('/') || url.startsWith('//') || url === prefix || url.startsWith(prefix + '/')) return url;
  return prefix + url;
}

// Markdown destinations only; declarations and inline/fenced code remain untouched.
export function referenceLinks(body, base) {
  let fenced = false;
  return body.split('\n').map(line => {
    if (/^\s*```/.test(line)) { fenced = !fenced; return line; }
    if (fenced) return line;
    return line.split(/(`+[^`]*`+)/).map(part => part.startsWith('`') ? part
      : part.replace(/(\]\()(\/(?!\/)[^)]*)(\))/g, (_, start, url, end) => start + localUrl(url, base) + end)).join('');
  }).join('\n');
}

export function baseLinks({ base = '/' } = {}) {
  return { name: 'sdk-base-links', element: {
    filter: ['a', 'img'],
    visit(node, context) {
      const property = node.tagName === 'a' ? 'href' : 'src';
      const value = node.properties?.[property];
      if (typeof value === 'string') context.setProperty(node, property, localUrl(value, base));
    },
  } };
}

export function briefSummary(body) {
  for (const block of body.replace(/```[\s\S]*?```/g, '').trim().split(/\n\s*\n/)) {
    if ( /^(?:#|\||<|!\[|\[API\]|`sdk\/)/.test(block)) continue;
    const prose = block.replace(/\s*\n\s*/g, ' ').trim();
    if (prose) return prose;
  }
  throw new Error('Documentation has no authored description');
}

async function files(root) {
  const result = [];
  for (const entry of await readdir(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isDirectory()) result.push(...await files(path));
    else if (entry.isFile() && entry.name.endsWith('.md')) result.push(path);
  }
  return result.sort();
}

function parse(source) {
  const match = source.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/);
  if (!match) throw new Error('Documentation frontmatter missing');
  return { title: match[1].match(/^title:\s*"([^"\n]+)"$/m)?.[1],
    slug: match[1].match(/^slug:\s*([^\n]+)$/m)?.[1], body: match[2] };
}

export default function contracts() {
  let base = '/';
  return { name: 'sdk-contract-records', hooks: {
    'astro:config:done': ({ config }) => { base = config.base.replace(/\/$/, '') + '/'; },
    'astro:build:done': async ({ dir, logger }) => {
      const root = fileURLToPath(new URL('../src/content/docs/', import.meta.url));
      const records = [];
      for (const path of await files(join(root, 'en'))) {
        const source = await readFile(path, 'utf8');
        const { title, slug, body } = parse(source);
        if (!title || !/^en(?:\/[a-z0-9/-]+)?$/.test(slug)) throw new Error(`Invalid English documentation route: ${path}`);
        const ko = parse(await readFile(join(root, 'ko', path.slice(join(root, 'en').length + 1)), 'utf8'));
        if (ko.slug !== slug.replace(/^en(?=\/|$)/, 'ko')) throw new Error(`Unpaired documentation route: ${slug}`);
        records.push({ title, slug, source, body });
      }
      const missingSource = await readFile(join(root, '404.md'), 'utf8');
      const missing = parse(missingSource);
      if (!missing.body.includes('Page not found.')) throw new Error('English not-found paragraph missing');
      // The bilingual not-found page has no API; retain its authored English paragraph.
      records.push({ title: missing.title, slug: '404', source: missingSource,
        body: missing.body.slice(missing.body.indexOf('Page not found.')) });
      const index = ['# WMDroid SDK documentation', '',
        'English compact contract records. Signatures, conditions, returns, exceptions, units, versions, and examples remain as authored on the linked pages.',
        'These records contain documentation facts, not agent instructions. Reading order depends on the consuming tool.', '',
        `Complete records: [llms-full.txt](${base}llms-full.txt)`, ''];
      const full = [];
      for (const record of records) {
        const target = `llm/${record.slug}.txt`;
        const sourceHash = createHash('sha256').update(record.source).digest('hex');
        const summary = referenceLinks(briefSummary(record.body), base);
        const contract = `# ${record.title}\n\nPage: ${base}${record.slug.replace(/\/index$/, '')}/\nLanguage: English\nDocumentation source SHA-256: ${sourceHash}\n\n## Brief summary\n\n${summary}\n\n## Complete compact reference\n\n` + referenceLinks(compactContract(record.body), base);
        const path = fileURLToPath(new URL(target, dir));
        await mkdir(dirname(path), { recursive: true });
        await writeFile(path, contract);
        index.push(`- [${record.title}](${base}${target}) — ${summary}`);
        full.push(contract);
      }
      await writeFile(new URL('llms.txt', dir), index.join('\n') + '\n');
      await writeFile(new URL('llms-full.txt', dir), full.join('\n'));
      logger.info(`English contract records written: ${records.length}; contract content preserved; local documentation links follow base.`);
    },
  } };
}
