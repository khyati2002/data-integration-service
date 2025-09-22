# Output for remote state

output "flink_application_arn" {
  description = "The ARN of the Flink application."
  value       = aws_kinesisanalyticsv2_application.flink_app.arn
}

output "iam_role_arn" {
  description = "The ARN of the IAM role for the Flink application."
  value       = aws_iam_role.flink_app.arn
}

output "cloudwatch_log_group_name" {
  description = "The name of the CloudWatch log group for the Flink application."
  value       = aws_cloudwatch_log_group.flink_app.name
}

output "cloudwatch_kms_key_arn" {
  description = "The ARN of the KMS key for CloudWatch logs, if created."
  value       = var.enable_cloudwatch_encryption ? aws_kms_key.cloudwatch_log_group_key[0].arn : null
}