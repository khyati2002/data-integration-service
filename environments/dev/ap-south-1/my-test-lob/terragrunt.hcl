include {
  path = find_in_parent_folders()
}
terraform {
  source = "../../../../../terraform"
}
inputs = {
  flink_app_name = "flink-app-my-test-lob"
}