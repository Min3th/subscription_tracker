variable "name_prefix" {
  description = "Prefix used for application resource names"
  type        = string
}

variable "subnet_id" {
  description = "Public subnet in which the EC2 instance is launched"
  type        = string
}

variable "security_group_id" {
  description = "Security group attached to the EC2 instance"
  type        = string
}

variable "database_security_group_id" {
  description = "Security group attached to the PostgreSQL database"
  type        = string
}

variable "database_secret_arn" {
  description = "ARN of the RDS-managed database credential secret"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.micro"
}

variable "root_volume_size" {
  description = "Size of the encrypted EC2 root volume in GiB"
  type        = number
  default     = 8

  validation {
    condition     = var.root_volume_size >= 8
    error_message = "The root volume must be at least 8 GiB"
  }
}

variable "ami_parameter_name" {
  description = "SSM parameter containing the current Amazon Linux 2023 AMI ID"
  type        = string
  default     = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

variable "public_ingress_cidrs" {
  description = "IPv4 CIDRs allowed to reach HTTP and HTTPS"
  type        = set(string)
  default     = ["0.0.0.0/0"]
}

variable "deployment_artifact_retention_days" {
  description = "Number of days deployment artifacts are retained"
  type        = number
  default     = 30
}

variable "deployment_bucket_force_destroy" {
  description = "Whether Terraform may delete a non-empty deployment bucket"
  type        = bool
  default     = false
}
