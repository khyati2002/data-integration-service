remote_state {
  backend = "s3"
  config = {
    encrypt        = true
    bucket         = "iceberg-poc-salescode"
    key            = "${path_relative_to_include()}/terraform.tfstate"
    region         = "ap-south-1"
  }
}
