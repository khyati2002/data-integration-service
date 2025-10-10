variable "flink_app_environment_variables" {
  type = map(map(any))
}

variable "flink_app_name" {
  type = string
  default = "flink-terraform-lifecycle"
}

variable "region" {
  type = string
  default = "us-east-1"
}

variable "flink_app_runtime_environment" {
  type = string
  default = "FLINK-1_20"
}

variable "flink_app_allow_non_restored_state" {
  type = string
  default = false
}

variable "s3_bucket_name" {
  type = string
}

variable "s3_file_key" {
  type = string
}

variable "flink_app_parallelism" {
  type    = number
  default = 1
}

variable "flink_app_parallelism_per_kpu" {
  type    = number
  default = 1
}

variable "flink_app_autoscaling_enabled" {
  type    = bool
  default = false
}

variable "flink_app_monitoring_log_level" {
  type    = string
  default = "INFO"
}

variable "flink_app_monitoring_metrics_level" {
  type    = string
  default = "APPLICATION"
}

variable "flink_app_snapshots_enabled" {
  type    = bool
  default = false
}

variable "flink_app_restore_type" {
  type = string
  default = "RESTORE_FROM_LATEST_SNAPSHOT"
}

variable "flink_app_snapshot_name" {
  type = string
  default = ""
}

variable "flink_app_start" {
  type    = bool
  default = false
}

variable "code_content_type" {
  type    = string
  default = "ZIPFILE"
}

variable "cloudwatch_log_retention" {
  type    = number
  default = 3
}

variable "enable_cloudwatch_encryption" {
  type    = bool
  default = false
  description = "Set to true to enable customer-managed KMS encryption for CloudWatch logs."
}

variable "subnet_ids" {
  type = string
}

variable "security_ids" {
  type = string
}
