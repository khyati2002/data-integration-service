include "root" {
  path = find_in_parent_folders()
}
terraform {
  source = "../../../../terraform_configs"
}
inputs = {
  flink_app_name = "dataintegration-test2"
  region = "ap-south-1"
  s3_bucket_name = "salescode-dev-uat"
  s3_file_key = "dataintegration/test2/test2-project.jar"
  flink_app_environment_variables = file("${get_terragrunt_dir()}/flink-common-properties.json")
  subnet_ids = "subnet-0e7a14dd3b85aa544"
  security_ids = "sg-057cea2c5f26bf934"
}