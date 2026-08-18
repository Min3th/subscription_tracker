output "provider_arn" {
  description = "ARN of the GitHub Actions OIDC provider"
  value       = aws_iam_openid_connect_provider.github.arn
}

output "deployment_role_arn" {
  description = "ARN of the role assumed by the GitHub deployment workflow"
  value       = aws_iam_role.github_deployment.arn
}
