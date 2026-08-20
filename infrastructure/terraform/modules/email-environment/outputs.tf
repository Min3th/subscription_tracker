output "ses_identity_arn" {
  description = "ARN of the development SES domain identity"
  value       = aws_sesv2_email_identity.domain.arn
}

output "ses_configuration_set_name" {
  description = "SES configuration set used by the application"
  value       = aws_sesv2_configuration_set.this.configuration_set_name
}

output "inbound_bucket_name" {
  description = "Bucket containing raw inbound MIME messages"
  value       = aws_s3_bucket.inbound.id
}

output "inbound_queue_url" {
  description = "URL of the inbound email queue"
  value       = aws_sqs_queue.inbound.url
}

output "inbound_queue_arn" {
  description = "ARN of the inbound email queue"
  value       = aws_sqs_queue.inbound.arn
}

output "inbound_dlq_url" {
  description = "URL of the inbound dead-letter queue"
  value       = aws_sqs_queue.inbound_dlq.url
}

output "event_queue_url" {
  description = "URL of the SES lifecycle-event queue"
  value       = aws_sqs_queue.events.url
}

output "event_queue_arn" {
  description = "ARN of the SES lifecycle-event queue"
  value       = aws_sqs_queue.events.arn
}

output "event_dlq_url" {
  description = "URL of the SES lifecycle-event dead-letter queue"
  value       = aws_sqs_queue.events_dlq.url
}

output "receipt_rule_set_name" {
  description = "Active SES receipt rule set"
  value       = aws_ses_receipt_rule_set.this.rule_set_name
}

output "inbound_mx_value" {
  description = "MX value required for the inbound domain"
  value       = "10 inbound-smtp.${data.aws_region.current.region}.amazonaws.com"
}

output "dkim_tokens" {
  description = "DKIM tokens that must be published in DNS"
  value       = aws_sesv2_email_identity.domain.dkim_signing_attributes[0].tokens
}
