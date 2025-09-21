const fs = require('fs');
const path = require('path');

// Helper to parse arguments like --key value
const parseArgs = () => {
  const args = {};
  process.argv.slice(2).forEach((arg, i, arr) => {
    if (arg.startsWith('--')) {
      const key = arg.substring(2);
      const next = arr[i + 1];
      args[key] = (next && !next.startsWith('--')) ? next : true;
    }
  });
  return args;
};

const main = () => {
  const args = parseArgs();
  const { lob, env, region } = args;

  if (!lob || !env || !region) {
    console.error('Usage: node scripts/setup-lob.js --lob <name> --env <env> --region <region>');
    process.exit(1);
  }

  // 1. Define paths and create directory
  const targetDir = path.join('environments', env, region, lob);
  fs.mkdirSync(targetDir, { recursive: true });
  console.log(`Created directory: ${targetDir}`);

  // 2. Generate terragrunt.hcl
  const terragruntContent = `
include {
  path = find_in_parent_folders()
}
terraform {
  source = "../../../../../terraform"
}
inputs = {
  flink_app_name = "flink-app-${lob}"
}
`;
  fs.writeFileSync(path.join(targetDir, 'terragrunt.hcl'), terragruntContent.trim());
  console.log('terragrunt.hcl created.');

  // 3. Generate flink-common-properties.json
  const templatePath = path.join('terraform', 'flink-common-properties.json');
  const envConfigPath = path.join('environments', env, region, 'config.json');

  if (!fs.existsSync(templatePath)) {
    console.error(`Error: Template file not found at ${templatePath}`);
    process.exit(1);
  }
  if (!fs.existsSync(envConfigPath)) {
    console.error(`Error: Environment config file not found at ${envConfigPath}`);
    process.exit(1);
  }

  let propertiesTemplate = fs.readFileSync(templatePath, 'utf8');
  const envConfig = JSON.parse(fs.readFileSync(envConfigPath, 'utf8'));

  // Replace environment-specific placeholders
  for (const [key, value] of Object.entries(envConfig)) {
    propertiesTemplate = propertiesTemplate.replace(new RegExp(`__${key.toUpperCase().replace('.', '\.')}__`, 'g'), value);
  }

  // Replace the LOB placeholder
  propertiesTemplate = propertiesTemplate.replace(/__LOB__/g, lob);

  fs.writeFileSync(path.join(targetDir, 'flink-common-properties.json'), propertiesTemplate);
  console.log('flink-common-properties.json created.');

  console.log(`
Scaffolding for LOB '${lob}' in '${env}/${region}' completed successfully.`);
};

main();
