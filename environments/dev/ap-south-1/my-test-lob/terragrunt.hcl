include "root" {
  path = find_in_parent_folders()
}
terraform {
  source = "../../../../terraform"
}
inputs = {
  flink_app_name = "dataintegration-my-test-lob"
}