data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "amazon_linux_ami" {
  name = var.ami_parameter_name
}

locals {
  deployment_bucket_name = lower(
    "${var.name_prefix}-deployments-${data.aws_caller_identity.current.account_id}"
  )
}

resource "aws_s3_bucket" "deployments" {
  bucket        = local.deployment_bucket_name
  force_destroy = var.deployment_bucket_force_destroy
  tags = {
    Name = local.deployment_bucket_name
  }
}

resource "aws_s3_bucket_ownership_controls" "deployments" {
  bucket = aws_s3_bucket.deployments.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_public_access_block" "deployments" {
  bucket = aws_s3_bucket.deployments.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "deployments" {
  bucket = aws_s3_bucket.deployments.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "deployments" {
  bucket = aws_s3_bucket.deployments.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "deployments" {
  bucket = aws_s3_bucket.deployments.id

  rule {
    id     = "expire-old-artifacts"
    status = "Enabled"
    filter {}

    noncurrent_version_expiration {
      noncurrent_days = var.deployment_artifact_retention_days
    }
  }

  depends_on = [aws_s3_bucket_versioning.deployments]

}

data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

resource "aws_iam_role" "application" {
  name               = "${var.name_prefix}-application"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json

  tags = {
    Name = "${var.name_prefix}-application-role"
  }
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.application.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "cloudwatch" {
  role       = aws_iam_role.application.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

data "aws_iam_policy_document" "application_runtime" {
  statement {
    sid    = "ReadDatabaseSecret"
    effect = "Allow"

    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetSecretValue"
    ]

    resources = [
      var.database_secret_arn,
    ]
  }

  statement {
    sid    = "ListDeploymentBucket"
    effect = "Allow"

    actions = [
      "s3:GetBucketLocation",
      "s3:ListBucket"
    ]

    resources = [
      aws_s3_bucket.deployments.arn
    ]
  }

  statement {
    sid    = "ReadDeploymentArtifacts"
    effect = "Allow"

    actions = [
      "s3:GetObject",
      "s3:GetObjectVersion"
    ]

    resources = ["${aws_s3_bucket.deployments.arn}/*"]
  }


  statement {
    sid    = "ReadApplicationSecret"
    effect = "Allow"

    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetSecretValue"
    ]

    resources = [
      aws_secretsmanager_secret.application.arn
    ]
  }

  statement {
    sid = "ReadRuntimeConfiguration"
    effect = "Allow"

    actions = ["ssm:GetParameter"]

    resources = ["arn:aws:ssm:ap-south-1:594559484604:parameter/subtrak/dev/runtime"]
  }

}

resource "aws_iam_role_policy" "application_runtime" {
  name   = "${var.name_prefix}-application-runtime"
  role   = aws_iam_role.application.id
  policy = data.aws_iam_policy_document.application_runtime.json
}

resource "aws_iam_instance_profile" "application" {
  name = "${var.name_prefix}-application"
  role = aws_iam_role.application.name
}

# Application ingress

resource "aws_vpc_security_group_ingress_rule" "http" {
  for_each = var.public_ingress_cidrs

  security_group_id = var.security_group_id
  description       = "Public HTTP access"

  cidr_ipv4   = each.value
  from_port   = 80
  to_port     = 80
  ip_protocol = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "https" {
  for_each = var.public_ingress_cidrs

  security_group_id = var.security_group_id
  description       = "Public HTTPS access"

  cidr_ipv4   = each.value
  from_port   = 443
  to_port     = 443
  ip_protocol = "tcp"
}

# Application egress

resource "aws_vpc_security_group_egress_rule" "postgresql" {
  security_group_id            = var.security_group_id
  referenced_security_group_id = var.database_security_group_id
  description                  = "PostgreSQL access to the dev database"

  from_port   = 5432
  to_port     = 5432
  ip_protocol = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "http" {
  security_group_id = var.security_group_id
  description       = "HTTP access for package installation and redirects"

  cidr_ipv4   = "0.0.0.0/0"
  from_port   = 80
  to_port     = 80
  ip_protocol = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "https" {
  security_group_id = var.security_group_id
  description       = "HTTPS access for AWS APIs and external services"

  cidr_ipv4   = "0.0.0.0/0"
  from_port   = 443
  to_port     = 443
  ip_protocol = "tcp"
}

# EC2 instance

resource "aws_instance" "application" {
  ami           = data.aws_ssm_parameter.amazon_linux_ami.value
  instance_type = var.instance_type
  subnet_id     = var.subnet_id

  vpc_security_group_ids = [var.security_group_id]

  iam_instance_profile        = aws_iam_instance_profile.application.name
  associate_public_ip_address = true

  user_data = <<-USER_DATA
    #!/bin/bash
    set -euxo pipefail

    dnf install -y java-17-amazon-corretto-headless jq nginx

    install -d \
       -o ec2-user \
       -g ec2-user \
       -m 0750 \
       /home/ec2-user/subtrak

    systemctl enable amazon-ssm-agent
    systemctl start amazon-ssm-agent
  USER_DATA

  user_data_replace_on_change = true

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
    instance_metadata_tags      = "disabled"
  }

  root_block_device {
    encrypted   = true
    volume_type = "gp3"
    volume_size = var.root_volume_size

    tags = {
      Name = "${var.name_prefix}-application-root"
    }
  }

  tags = {
    Name = "${var.name_prefix}-application"
  }

  depends_on = [
    aws_iam_role_policy.application_runtime,
    aws_iam_role_policy_attachment.ssm
  ]
}

resource "aws_eip" "application" {
  domain   = "vpc"
  instance = aws_instance.application.id

  tags = {
    Name = "${var.name_prefix}-application-eip"
  }
}

data "aws_iam_policy_document" "deployment_bucket" {
  statement {
    sid    = "DenyInsecureTransport"
    effect = "Deny"

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    actions = ["s3:*"]

    resources = [
      aws_s3_bucket.deployments.arn,
      "${aws_s3_bucket.deployments.arn}/*"
    ]

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }

}

resource "aws_s3_bucket_policy" "deployments" {
  bucket = aws_s3_bucket.deployments.id
  policy = data.aws_iam_policy_document.deployment_bucket.json

  depends_on = [aws_s3_bucket_public_access_block.deployments]

}

resource "aws_secretsmanager_secret" "application" {
  name                    = "${var.name_prefix}/application"
  recovery_window_in_days = 7

  tags = {
    Name = "${var.name_prefix}-application-secret"
  }
}
