resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = [
    "sts.amazonaws.com",
  ]

  tags = {
    Name = "${var.name_prefix}-github-actions"
  }
}

data "aws_iam_policy_document" "github_assume_role" {
  statement {
    sid     = "GitHubEnvironmentDeployment"
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values = [
        "repo:${var.github_repository_owner}/${var.github_repository_name}:environment:${var.github_environment}"
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:ref"
      values   = ["refs/heads/${var.github_branch}"]
    }
  }
}

resource "aws_iam_role" "github_deployment" {
  name                 = "${var.name_prefix}-github-deployment"
  assume_role_policy   = data.aws_iam_policy_document.github_assume_role.json
  max_session_duration = 3600

  tags = {
    Name = "${var.name_prefix}-github-deployment-role"
  }
}

data "aws_iam_policy_document" "github_deployment" {
  statement {
    sid    = "UploadDeploymentArtifacts"
    effect = "Allow"

    actions = [
      "s3:PutObject",
    ]

    resources = [
      "${var.deployment_bucket_arn}/subscription-service.jar",
      "${var.deployment_bucket_arn}/subscription-service.jar.sha256",
      "${var.deployment_bucket_arn}/runtime/install-runtime-from-s3.sh",
      "${var.deployment_bucket_arn}/runtime/runtime-assets.tar.gz",
      "${var.deployment_bucket_arn}/runtime/runtime-assets.tar.gz.sha256",
    ]
  }

  statement {
    sid     = "RunDeploymentOnDevInstance"
    effect  = "Allow"
    actions = ["ssm:SendCommand"]

    resources = [
      var.application_instance_arn,
      "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript",
    ]
  }

  statement {
    sid    = "ReadDeploymentCommandResult"
    effect = "Allow"

    actions = [
      "ssm:GetCommandInvocation",
    ]

    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_deployment" {
  name   = "${var.name_prefix}-github-deployment"
  role   = aws_iam_role.github_deployment.id
  policy = data.aws_iam_policy_document.github_deployment.json
}
