include "root" {
  path = find_in_parent_folders()
}
terraform {
  source = "../../../../terraform"
}
inputs = {
  flink_app_name = "dataintegration-my-test-lob"
  region = "ap-south-1"
  s3_bucket_name = "salescode-dev-uat"
  s3_file_key = "dataintegration/my-test-lob/my-test-lob-project.jar"
}