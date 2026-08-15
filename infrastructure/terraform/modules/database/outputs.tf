output "identifier" {
  description = "RDS database identifier"
  value       = aws_db_instance.this.identifier
}

output "arn" {
  description = "ARN of the RDS database"
  value       = aws_db_instance.this.arn
}

output "address" {
  description = "RDS hostname without the port"
  value       = aws_db_instance.this.address
}

output "endpoint" {
  description = "RDS hostname and port"
  value       = aws_db_instance.this.endpoint
}

output "port" {
  description = "PostgreSQL port"
  value       = aws_db_instance.this.port
}

output "database_name" {
  description = "Name of the initial PostgreSQL database"
  value       = aws_db_instance.this.db_name
}

output "security_group_id" {
  description = "ID of the database security group"
  value       = aws_security_group.this.id
}

output "master_user_secret_arn" {
  description = "ARN of the RDS-managed master credential secret"
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}
