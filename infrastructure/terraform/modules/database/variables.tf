variable "name_prefix" {
  description = "Prefix used for database resource names"
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC containing the database"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs used by the RDS subnet group"
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_ids) >= 2
    error_message = "RDS requires private subnets in at least two availability zones"
  }
}

variable "allowed_security_group_ids" {
  description = "Security group IDs allowed to connect to PostgreSQL"
  type        = map(string)
  default     = {}
}

variable "database_name" {
  description = "Name of the initial PostgreSQL database"
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9_]*$", var.database_name))
    error_message = "The database name must start with a letter and contain only letters, numbers, and underscores."
  }
}

variable "master_username" {
  description = "Master PostgreSQL username"
  type        = string
  default     = "subtrak_admin"

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9_]*$", var.master_username))
    error_message = "The master username must start with a letter and contain only letters, numbers, and underscores."
  }
}

variable "engine_version" {
  description = "PostgreSQL engine version"
  type        = string
  default     = "18.3"
}

variable "instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "allocated_storage" {
  description = "Initial database storage in GiB"
  type        = number
  default     = 20

  validation {
    condition     = var.allocated_storage >= 20
    error_message = "Allocated storage must be at least 20 GiB."
  }
}

variable "max_allocated_storage" {
  description = "Maximum autoscaled storage in GiB"
  type        = number
  default     = 50
}

variable "backup_retention_days" {
  description = "Number of days automated database backups are retained"
  type        = number
  default     = 1

  validation {
    condition     = var.backup_retention_days >= 0 && var.backup_retention_days <= 35
    error_message = "Backup retention must be between 0 and 35 days"
  }
}

variable "multi_az" {
  description = "Whether RDS uses a Multi-AZ deployment"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Whether RDS deletion protection is enabled"
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  description = "Whether to skip the final snapshot when deleting the database"
  type        = bool
  default     = true
}

variable "apply_immediately" {
  description = "Whether database changes are applied immediately"
  type        = bool
  default     = true
}
