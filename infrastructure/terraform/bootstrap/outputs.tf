# Exposes useful values after terraform apply

output "state_bucket_name" {
  description = "Name of the Terraform state bucket"
  value       = aws_s3_bucket.terraform_state.id
}

# ARN = Amazon Resource Name

output "state_bucket_arn" {
  description = "ARN of the Terraform state bucket"
  value       = aws_s3_bucket.terraform_state.arn
}

output "aws_account_id" {
  description = "AWS account contatining the state infrastructure"
  value       = data.aws_caller_identity.current.account_id
}
