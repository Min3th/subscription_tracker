variable "name_prefix" {
  description = "Prefix used for email resource names"
  type = string
}

variable "email_domain" {
  description = "SES identity used for outbound email"
  type = string

  validation {
    condition = can(regex("^[A-Za-z0-9.-]+$", var.email_domain))
    error_message = "The email domain must be a valid DNS name."
  }
}

variable "inbound_domain" {
  description = "Complete domain receiving forwarded email"
  type = string

  validation {
    condition = can(regex("^[A-Za-z0-9.-]+$", var.inbound_domain))
    error_message = "The inbound domain must be a valid DNS name."
  }
}

variable "application_role_name" {
  description = "IAM role used by the application EC2 instance"
  type = string
}

variable "raw_mime_retention_days" {
  description = "Number of days raw inbound messages remain in S3"
  type = number
  default = 8

  validation {
    condition = var.raw_mime_retention_days >= 2
    error_message = "Raw MIME retention must include at least one retention day and one safety day."
  }
}

# How long sqs retains unprocessed message
variable "queue_message_retention_seconds" {
  description = "SQS message retention peiod"
  type = number
  default = 1209600 # 14 days
}

# temporarily make message invisible till worker works on it after receiving
variable "queue_visibility_timeout_seconds" {
  description = "SQS visibility timeout"
  type = number
  default = 120
}

variable "max_receive_count" {
  description = "Number of receives before moving a message to its DLQ"
  type = number
  default = 5
}
