#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const input = process.argv[2];

if (!input) {
  console.error("Usage: node create-cypress-module.js <folder-or-path>");
  console.error("Example: node create-cypress-module.js volunteer/test-two");
  process.exit(1);
}

// Full directory path (can be nested like "volunteer/test-two")
const targetDir = path.resolve(process.cwd(), input);

// Folder name (last part) used as file prefix
const baseName = path.basename(input);

if (!fs.existsSync(targetDir)) {
  fs.mkdirSync(targetDir, { recursive: true });
  console.log(`Created folder: ${targetDir}`);
} else {
  console.log(`Folder already exists: ${targetDir}`);
}

const files = [
  `${baseName}.feature`,
  `${baseName}.po.ts`,
  `${baseName}.selectors.ts`,
  `${baseName}.steps.ts`,
  `${baseName}.workflow.ts`,
];

for (const file of files) {
  const fullPath = path.join(targetDir, file);

  if (!fs.existsSync(fullPath)) {
    fs.writeFileSync(fullPath, "");
    console.log(`Created file: ${fullPath}`);
  } else {
    console.log(`Skipped (already exists): ${fullPath}`);
  }
}
