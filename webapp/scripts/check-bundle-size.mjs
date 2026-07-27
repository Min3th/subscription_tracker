import { readFile, readdir } from "node:fs/promises";
import { gzipSync } from "node:zlib";
import path from "node:path";

const DIST_DIRECTORY = path.resolve("dist");
const INITIAL_JS_BUDGET = 130 * 1024;
const ASYNC_JS_BUDGET = 110 * 1024;

const indexHtml = await readFile(path.join(DIST_DIRECTORY, "index.html"), "utf8");
const entryMatch = indexHtml.match(
  /<script\b[^>]*\btype="module"[^>]*\bsrc="([^"]+)"/i,
);

if (!entryMatch) {
  throw new Error("Could not find the initial module script in dist/index.html");
}

const entryFile = entryMatch[1].replace(/^\//, "");
const assetFiles = await readdir(path.join(DIST_DIRECTORY, "assets"));
const javascriptFiles = assetFiles
  .filter((file) => file.endsWith(".js"))
  .map((file) => `assets/${file}`);

let failed = false;

for (const relativeFile of javascriptFiles) {
  const contents = await readFile(path.join(DIST_DIRECTORY, relativeFile));
  const gzipBytes = gzipSync(contents, { level: 9 }).byteLength;
  const isEntry = relativeFile === entryFile;
  const budget = isEntry ? INITIAL_JS_BUDGET : ASYNC_JS_BUDGET;
  const status = gzipBytes <= budget ? "PASS" : "FAIL";

  console.log(
    `${status} ${relativeFile}: ${(gzipBytes / 1024).toFixed(1)} KiB gzip ` +
      `(budget: ${budget / 1024} KiB)`,
  );

  failed ||= gzipBytes > budget;
}

if (failed) {
  console.error("Bundle size budget exceeded.");
  process.exitCode = 1;
} else {
  console.log("All JavaScript bundles are within budget.");
}
