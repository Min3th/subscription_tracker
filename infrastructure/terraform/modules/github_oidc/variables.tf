variable "name_prefix" {
  description = "Prefix used for GitHub deployment IAM resources"
  type        = string
}

variable "aws_region" {
  description = "AWS region containing the deployment target"
  type        = string
}

variable "github_repository_owner" {
  description = "GitHub organization or user that owns the repository"
  type        = string
}

variable "github_repository_name" {
  description = "GitHub repository name without the .git suffix"
  type        = string
}

variable "github_environment" {
  description = "GitHub deployment environment allowed to assume the role"
  type        = string
}

variable "github_branch" {
  description = "Git branch allowed to deploy to the environment"
  type        = string
}

variable "deployment_bucket_arn" {
  description = "ARN of the deployment artifact bucket"
  type        = string
}

variable "application_instance_arn" {
  description = "ARN of the EC2 instance that receives deployment commands"
  type        = string
}
