variable "name_prefix" {
  description = "Prefix used for network resource names"
  type = string
}

variable "vpc_cidr" {
  description = "CIDR block for the vpc"
  type = string
}

variable "availability_zones" {
  description = "Availability zones used for subnets"
  type = list(string)

  validation {
    condition = length(var.availability_zones) >= 2
    error_message = "At least two availability zones must be provided"
  }
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type = list(string)
}
