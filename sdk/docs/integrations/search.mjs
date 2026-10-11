import { mkdir, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gunzipSync } from 'node:zlib';
import * as pagefind from 'pagefind';

function checked(response) {
  if (response.errors.length) throw new Error(response.errors.join('\n'));
  return response;
}

export default function search() {
  return {
    name: 'sdk-search',
    hooks: {
      'astro:build:done': async ({ dir, logger }) => {
        try {
          const { index } = checked(await pagefind.createIndex());
          const { page_count } = checked(await index.addDirectory({ path: fileURLToPath(dir) }));
          const { files } = checked(await index.getFiles());
          // shortcut: Pagefind 1.5.2 writeFiles can acknowledge before flush; use it once shutdown preserves complete files.
          for (const file of files) {
            if (file.path.startsWith('wasm.')) gunzipSync(file.content);
            const target = fileURLToPath(new URL(`pagefind/${file.path}`, dir));
            await mkdir(dirname(target), { recursive: true });
            await writeFile(target, file.content);
          }
          logger.info(`Search index written completely: ${page_count} pages, ${files.length} files.`);
        } finally {
          await pagefind.close();
        }
      },
    },
  };
}
