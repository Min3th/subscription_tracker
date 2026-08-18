variable "aws_region" {
  description = "Primary AWS region for the dev environment"
  type        = string
}

variable "ses_region" {
  description = "AWS region used for SES resources"
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  description = "Project name used in resource names and tags"
  type        = string
  default     = "subtrak"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"

  validation {
    condition     = var.environment == "dev"
    error_message = "This Terraform root may only manage the dev environment"
  }

}

variable "vpc_cidr" {
  description = "CIDR block allocated to the dev VPC"
  type        = string
}

variable "availability_zones" {
  description = "Availability zones used by the dev environment"
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
}

variable "application_instance_type" {
  description = "EC2 instance type for the dev backend"
  type        = string
}

variable "database_instance_class" {
  description = "RDS instance class for the dev database"
  type        = string
}

variable "database_name" {
  description = "Initial PostgreSQL database name"
  type        = string
  default     = "subtrak"
}

variable "inbound_subdomain" {
  description = "Inbound email hostname for dev"
  type        = string
}

variable "inbound_retention_days" {
  description = "Number of days inbound MIME objects are retained"
  type        = number
  default     = 8

  validation {
    condition     = var.inbound_retention_days >= 1
    error_message = "Inbound retention must be at least one day"
  }
}

variable "github_repository_owner" {
  description = "GitHub user or organization that owns the deployment repository"
  type        = string
  default     = "Min3th"
}

variable "github_repository_name" {
  description = "GitHub repository name without the .git suffix"
  type        = string
  default     = "subscription_tracker"
}

variable "github_deployment_environment" {
  description = "GitHub environment authorized to deploy dev"
  type        = string
  default     = "development"
}

variable "github_deployment_branch" {
  description = "Git branch authorized to deploy dev"
  type        = string
  default     = "dev"
}
