output "instance_id" {
  description = "ID of the application EC2 instance"
  value       = aws_instance.application.id
}

output "instance_arn" {
  description = "ARN of the application EC2 instance"
  value       = aws_instance.application.arn
}

output "public_ip" {
  description = "Elastic IP assigned to the application"
  value       = aws_eip.application.public_ip
}

output "private_ip" {
  description = "Private IP of the application EC2 instance"
  value       = aws_instance.application.private_ip
}

output "iam_role_name" {
  description = "Name of the application IAM role"
  value       = aws_iam_role.application.name
}

output "iam_role_arn" {
  description = "ARN of the application IAM role"
  value       = aws_iam_role.application.arn
}

output "instance_profile_name" {
  description = "Name of the EC2 instance profile"
  value       = aws_iam_instance_profile.application.name
}

output "deployment_bucket_name" {
  description = "Name of the deployment artifact bucket"
  value       = aws_s3_bucket.deployments.id
}

output "deployment_bucket_arn" {
  description = "ARN of the deployment artifact bucket"
  value       = aws_s3_bucket.deployments.arn
}
