output "database_endpoint" {
  description = "Endpoint of the dev PostgreSQL database"
  value       = module.database.endpoint
}

output "database_name" {
  description = "Name of the dev PostgreSQL database"
  value       = module.database.database_name
}

output "database_secret_arn" {
  description = "ARN of the RDS-managed database credential secret"
  value       = module.database.master_user_secret_arn
}

output "application_instance_id" {
  description = "ID of the dev backend EC2 instance"
  value       = module.application.instance_id
}

output "application_public_ip" {
  description = "Public IP of the dev backend"
  value       = module.application.public_ip
}

output "deployment_bucket_name" {
  description = "Name of the dev deployment bucket"
  value       = module.application.deployment_bucket_name
}

output "application_secret_arn" {
  description = "ARN of the dev application runtime secret"
  value       = module.application.application_secret_arn
}

output "github_oidc_provider_arn" {
  description = "ARN of the GitHub Actions OIDC provider in the dev account"
  value       = module.github_oidc.provider_arn
}

output "github_deployment_role_arn" {
  description = "Role ARN to configure as the dev GitHub environment AWS_ROLE_ARN variable"
  value       = module.github_oidc.deployment_role_arn
}
