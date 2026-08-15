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
