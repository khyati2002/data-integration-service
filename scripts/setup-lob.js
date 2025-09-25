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

  // Load environment config
  const envConfigPath = path.join('environments', env, region, 'config.json');
  if (!fs.existsSync(envConfigPath)) {
    console.error(`Error: Environment config file not found at ${envConfigPath}`);
    process.exit(1);
  }
  const envConfig = JSON.parse(fs.readFileSync(envConfigPath, 'utf8'));

  // 2. Generate flink-common-properties.json
  const templatePath = path.join('terraform_configs', 'flink-common-properties.json');

  if (!fs.existsSync(templatePath)) {
    console.error(`Error: Template file not found at ${templatePath}`);
    process.exit(1);
  }

  let propertiesTemplate = fs.readFileSync(templatePath, 'utf8');
  
  const finalFlinkConfig = { ...envConfig, ...flinkPropertiesConfig };

  // Replace environment-specific placeholders
  for (const [key, value] of Object.entries(finalFlinkConfig)) {
    propertiesTemplate = propertiesTemplate.replace(new RegExp(`__${key.toUpperCase().replace('.', '\\.')}__`, 'g'), value);
  }

  // Replace the LOB placeholder
  propertiesTemplate = propertiesTemplate.replace(/__LOB__/g, lob);

  const flinkPropertiesPath = path.join(targetDir, 'flink-common-properties.json');
  fs.writeFileSync(flinkPropertiesPath, propertiesTemplate);
  console.log('flink-common-properties.json created.');

  // 3. Generate terragrunt.hcl
  const terragruntInputs = {
    flink_app_name: `dataintegration-${lob}`,
    region: region,
    s3_bucket_name: envConfig.s3_bucket_name,
    s3_file_key: `dataintegration/${lob}/${lob}-project.jar`,
    ...terragruntInputsConfig,
    // This must be the last entry, so it is not overridden by terragruntInputsConfig
    flink_app_environment_variables: 'file(\"${get_terragrunt_dir()}/flink-common-properties.json\")',
    subnet_ids: envConfig.subnet_ids,
    security_ids: envConfig.security_ids
  };

  const inputsContent = Object.entries(terragruntInputs)
    .map(([key, value]) => {
        if (typeof value === 'string') {
            // The value for flink_app_environment_variables is a raw HCL expression,
            // not a string that needs to be quoted.
            if (key === 'flink_app_environment_variables') {
                return `  ${key} = ${value}`;
            }
            return `  ${key} = \"${value}\"`;
        }
        // For any other complex types, stringify them.
        return `  ${key} = ${JSON.stringify(value)}`;
    })
    .join('\n');

  const terragruntContent = `
include "root" {
  path = find_in_parent_folders()
}
terraform {
  source = "../../../../terraform_configs"
}
inputs = {
${inputsContent}
}
`.trim();

  fs.writeFileSync(path.join(targetDir, 'terragrunt.hcl'), terragruntContent);
  console.log('terragrunt.hcl created.');

  console.log(`
Scaffolding for LOB '${lob}' in '${env}/${region}' completed successfully.`);
};

main();
