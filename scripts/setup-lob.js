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
  const terragruntInputsOverride = args['terragrunt-inputs'];
  const flinkPropertiesOverride = args['flink-properties'];


  if (!lob || !env || !region) {
    console.error('Usage: node scripts/setup-lob.js --lob <name> --env <env> --region <region> [--terragrunt-inputs {"key": "value"}] [--flink-properties {"key": "value"}]');
    process.exit(1);
  }

  let terragruntInputsConfig = {};
  if (terragruntInputsOverride) {
    try {
      terragruntInputsConfig = JSON.parse(terragruntInputsOverride);
    } catch (e) {
      console.error('Error: --terragrunt-inputs argument is not a valid JSON string.');
      process.exit(1);
    }
  }

  let flinkPropertiesConfig = {};
  if (flinkPropertiesOverride) {
    try {
      flinkPropertiesConfig = JSON.parse(flinkPropertiesOverride);
    } catch (e) {
      console.error('Error: --flink-properties argument is not a valid JSON string.');
      process.exit(1);
    }
  }

  // 1. Define paths and create directory
  const targetDir = path.join('environments', env, region, lob);
  fs.mkdirSync(targetDir, { recursive: true });
  console.log(`Created directory: ${targetDir}`);

  // 2. Generate terragrunt.hcl
  const terragruntInputs = {
    flink_app_name: `flink-app-${lob}`,
    ...terragruntInputsConfig
  };

  const inputsContent = Object.entries(terragruntInputs)
    .map(([key, value]) => {
        if (typeof value === 'string') {
            return `  ${key} = "${value}"`;
        }
        return `  ${key} = ${value}`;
    })
    .join('\n');

  const terragruntContent = `
include "root" {
  path = find_in_parent_folders()
}
terraform {
  source = "../../../../../../terraform"
}
inputs = {
${inputsContent}
}
`.trim();

  fs.writeFileSync(path.join(targetDir, 'terragrunt.hcl'), terragruntContent);
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

  const finalFlinkConfig = { ...envConfig, ...flinkPropertiesConfig };

  // Replace environment-specific placeholders
  for (const [key, value] of Object.entries(finalFlinkConfig)) {
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