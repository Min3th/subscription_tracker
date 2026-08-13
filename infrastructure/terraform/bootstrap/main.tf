data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

locals {
  common_tags = {
    Application = "SubTrak"
    Environment = var.environment
    ManagedBy   = "Terraform"
    Purpose     = "TerraformState"
  }
}

# Terraform state bucket

resource "aws_s3_bucket" "terraform_state" {
  bucket = var.state_bucket_name

  lifecycle {
    prevent_destroy = true
  }

  tags = merge(local.common_tags, {
    Name = var.state_bucket_name
  })
}

# Disable ACL-based ownership and make the bucket owner own all objects
resource "aws_s3_bucket_ownership_controls" "terraform_state" {
  # bucket = var.state_bucket_name
  bucket = aws_s3_bucket.terraform_state.id # this makes sure that terraform understands that the bucket must exist before making config changes

  lifecycle {
    prevent_destroy = true
  }

  # tags = merge(local.common_tags,{
  #   Name = var.state_bucket_name
  # })

  rule {
    object_ownership = "BucketOwnerEnforced"
  }

}

# Encryption at rest

resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {

  bucket = aws_s3_bucket.terraform_state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }

}

resource "aws_s3_bucket_policy" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  policy = data.aws_iam_policy_document.terraform_state.json

  depends_on = [aws_s3_bucket_public_access_block.terraform_state]
}

resource "aws_s3_bucket_public_access_block" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Bucket policy - deny requests made without https/tls

data "aws_iam_policy_document" "terraform_state" {

  statement {
    sid    = "DenyInsecureTransport" # statement id
    effect = "Deny"                  # deny when condition matches

    principals {
      type        = "*"
      identifiers = ["*"] # this means that the rule applies to all
    }

    actions = [
      "s3:*" # covers all s3 operations
    ]

    resources = [
      aws_s3_bucket.terraform_state.arn,       # refers to the bucket itself
      "${aws_s3_bucket.terraform_state.arn}/*" # refers to all objects insde the bucket
    ]

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }

  # Request to S3 -> Is aws:SecureTransport false? -> if yes -> DENY
}

resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  versioning_configuration {
    status = "Enabled"
  }
}

