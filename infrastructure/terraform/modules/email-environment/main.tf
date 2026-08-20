data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

locals {
  inbound_bucket_name = lower(
    "${var.name_prefix}-ses-inbound-${data.aws_caller_identity.current.account_id}"
  )
}

resource "aws_s3_bucket" "inbound" {
  bucket = local.inbound_bucket_name

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_ownership_controls" "inbound" {
  bucket = aws_s3_bucket.inbound.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_public_access_block" "inbound" {
  bucket = aws_s3_bucket.inbound.id
  
  block_public_acls = true
  block_public_policy = true
  ignore_public_acls = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "inbound" {
  bucket = aws_s3_bucket.inbound.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "inbound" {
  bucket = aws_s3_bucket.inbound.id
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "inbound" {
  bucket = aws_s3_bucket.inbound.id

  rule {
    id = "expire-raw-mime"
    status = "Enabled"

    filter {
      prefix = "incoming/"
    }

    expiration {
      days = var.raw_mime_retention_days
    }

    noncurrent_version_expiration {
      noncurrent_days = 1
    }
  }

  depends_on = [ aws_s3_bucket_versioning.inbound ]
}

data "aws_iam_policy_document" "inbound_bucket" {
  statement {
    sid = "DenyInsecureTransport"
    effect = "Deny"

    principals {
      type = "*"
      identifiers = ["*"]
    }

    actions = ["s3:*"] # for all actions like s3:GetObject,s3:PutObject.s3:DeleteObject,s3:ListBucket

    resources = [
        aws_s3_bucket.inbound.arn,
        "${aws_s3_bucket.inbound.arn}/*", # Cover both bucket , and the things insdie it
    ]

    condition {
      test = "Bool"
      variable = "aws:SecureTransport"
      values = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "inbound" {
  bucket = aws_s3_bucket.inbound.id
  policy = data.aws_iam_policy_document.inbound_bucket.json
}

resource "aws_sqs_queue" "inbound_dlq" {
  name = "${var.name_prefix}-ses-inbound-dlq"
  message_retention_seconds = var.queue_message_retention_seconds
  sqs_managed_sse_enabled = true
}

resource "aws_sqs_queue" "inbound" {
  name = "${var.name_prefix}-ses-inbound"
  message_retention_seconds = var.queue_message_retention_seconds
  visibility_timeout_seconds = var.queue_visibility_timeout_seconds
  receive_wait_time_seconds = 20
  sqs_managed_sse_enabled = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.inbound_dlq.arn
    maxReceiveCount = var.max_receive_count
  })
}

resource "aws_sqs_queue" "events_dlq" {
  name = "${var.name_prefix}-ses-events-dlq"
  message_retention_seconds = var.queue_message_retention_seconds
  sqs_managed_sse_enabled = true
}

resource "aws_sqs_queue" "events" {
  name = "${var.name_prefix}-ses-events"
  message_retention_seconds = var.queue_message_retention_seconds
  visibility_timeout_seconds = var.queue_visibility_timeout_seconds
  receive_wait_time_seconds = 20
  sqs_managed_sse_enabled = true

  # Redrive policy expects a json object
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.events.dlq.arn
    maxReceiveCount = var.max_receive_count
  })
}

resource "aws_sns_topic" "inbound" {
  name = "${var.name_prefix}-ses-inbound" 
}

resource "aws_sns_topic" "events" {
  name = "${var.name_prefix}-ses-events"
}

resource "aws_sns_topic_subscription" "inbound_queue" {
  topic_arn = aws_sns_topic.inbound.arn
  protocol = "sqs"
  endpoint = aws_sqs_queue.inbound.arn
  raw_message_delivery = false
}

resource "aws_sns_topic_subscription" "event_queue" {
  topic_arn = aws_sns_topic.events.arn
  protocol = "sqs"
  endpoint = aws_sqs_queue.events.arn
  raw_message_delivery = false
}

data "aws_iam_policy_document" "inbound_queue" {
  statement {
    sid = "AllowInboundTopic"
    effect = "Allow"

    principals {
      type = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    actions = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.inbound.arn]

    condition {
      test = "ArnEquals"
      variable = "aws:SourceArn"
      values = [aws_sns_topic.inbound.arn]
    }

    condition {
      test = "StringEquals"
      variable = "aws:SourceAccount"
      values = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_sqs_queue_policy" "inbound" {
  queue_url = aws_sqs_queue.inbound.id
  policy = data.aws_iam_policy_document.inbound_queue.json
}

data "aws_iam_policy_document" "event_queue" {
  statement {
    sid = "AllowEventTopic"
    effect = "Allow"

    # Entity allowed to perform the action
    principals {
      type = "Service"
      identifiers = ["sns:amazonaws.com"]
    }

    actions = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.events.arn]

    # The request must come specifically from the events SNS topic
    condition {
      test = "ArnEquals"
      variable = "aws:SourceArn"
      values = [aws_sns_topic.events.arn]
    }

    # The SNS topic making the request must belong to current account
    condition {
      test = "StringEquals"
      variable = "aws:SourceAccount"
      values = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_sqs_queue_policy" "events" {
  queue_url = aws_sqs_queue.events.id
  policy = data.aws_iam_policy_document.event_queue.json
}

data "aws_iam_policy_document" "ses_assume_role" {
  statement {
    effect = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type = "Service"
      identifiers = ["ses.amazonaws.com"]
    }

    condition {
      test = "StringEquals"
      variable = "AWS:SourceAccount"
      values = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_iam_role" "ses_receipt" {
  name = "${var.name_prefix}-ses-receipt"
  assume_role_policy = data.aws_iam_policy_document.ses_assume_role.json
}

# Principal determined by the role this is attached to
data "aws_iam_policy_document" "ses_receipt" {
  statement {
    sid = "StoreInboundEmail"
    effect = "Allow"
    actions = ["s3:PutObject"]
    resources = ["${aws_s3_bucket.inbound.arn}/incoming/*"]
  }

  statement {
    sid = "PublishInboundNotification"
    effect = "Allow"
    actions = ["sns:Publish"]
    resources = [aws_sns_topic.inbound.arn]
  }
}

resource "aws_iam_role_policy" "ses_receipt" {
  name = "${var.name_prefix}-ses-receipt"
  role = aws_iam_role.ses_receipt.id
  policy = data.aws_iam_policy_document.ses_receipt.json
}

data "aws_iam_policy_document" "inbound_topic" {
  statement {
    sid = "AllowSesReceiptRole"
    effect = "Allow"

    principals {
      type = "AWS"
      identifiers = [aws_iam_role.ses_receipt.arn]
    }

    actions = ["sns:Publish"]
    resources = [aws_sns_topic.inbound.arn]
  }
}

resource "aws_sns_topic_policy" "inbound" {
  arn = aws_sns_topic.inbound.arn
  policy = data.aws_iam_policy_document.inbound_topic.json
}

resource "aws_sesv2_email_identity" "domain" {
  email_identity = var.email_domain
}

resource "aws_sesv2_configuration_set" "this" {
  configuration_set_name = var.name_prefix

  reputation_options {
    reputation_metrics_enabled = true
  }

  sending_options {
    sending_enabled = true 
  }
}

data "aws_iam_policy_document" "event_topic" {
  statement {
    sid = "AlloSesConfigurationSet"
    effect = "Allow"

    principals {
      type = "Service"
      identifiers = ["ses.amazonaws.com"]
    }

    actions = ["sns:Publish"]
    resources = [aws_sns_topic.events.arn]

    condition {
      test = "StringEquals"
      variable = "AWS:SourceAccount"
      values = [data.aws_caller_identity.current.account_id]
    }

    condition {
      test = "ArnEquals"
      variable = "AWS:SourceArn"
      values = [
        "arn:${data.aws_partition.current.partition}:ses:${data.aws_region.current.region}:${data.aws_caller_identity.current.account_id}:configuration-set/${var.name_prefix}"
      ]
    }
  }
}

resource "aws_sns_topic_policy" "events" {
  arn    = aws_sns_topic.events.arn
  policy = data.aws_iam_policy_document.event_topic.json
}

data "aws_region" "current" {}

resource "aws_sesv2_configuration_set_event_destination" "events" {
  configuration_set_name = aws_sesv2_configuration_set.this.configuration_set_name
  event_destination_name = "lifecycle-events"

  event_destination {
    enabled = true 
    matching_event_types = ["BOUNCE", "COMPLAINT", "DELIVERY"]

    sns_destination {
      topic_arn = aws_sns_topic.events.arn
    }
  }
  
  depends_on = [ aws_sns_topic_policy.events ]
}

resource "aws_ses_receipt_rule_set" "this" {
  rule_set_name = "${var.name_prefix}-inbound"
}

resource "aws_ses_receipt_rule" "inbound" {
  name = "${var.name_prefix}-store-inbound"
  rule_set_name = aws_ses_receipt_rule_set.this.rule_set_name

  enabled = true 
  scan_enabled = true # to check for viruses
  tls_policy = "Require"

  recipients = [var.inbound_domain]

  s3_action {
    bucket_name = aws_s3_bucket.inbound.id 
    object_key_prefix = "incoming/" # stores under /incoming
    topic_arn = aws_sns_topic.inbound.arn
    iam_role_arn = aws_iam_role.ses_receipt.arn
    position = 1
  }

   depends_on = [
    aws_s3_bucket_policy.inbound,
    aws_sns_topic_policy.inbound,
    aws_iam_role_policy.ses_receipt,
  ]
}

resource "aws_ses_active_receipt_rule_set" "this" {
  rule_set_name = aws_ses_receipt_rule_set.this.rule_set_name

  depends_on = [ aws_ses_receipt_rule.inbound ]
}

data "aws_iam_policy_document" "application_email" {
  statement {
    sid = "SendEmail"
    effect = "Allow"
    actions = ["ses:SendEmail"]
    resources = [aws_sesv2_email_identity.domain.arn]
  }

  statement {
    sid = "ConsumeInboundQueue"
    effect = "Allow"

    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueAttributes",
    ]

    resources = [aws_sqs_queue.inbound.arn]
  }

  statement {
    sid    = "ConsumeEventQueue"
    effect = "Allow"

    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueAttributes",
    ]

    resources = [aws_sqs_queue.events.arn]
  }

  statement {
    sid = "ReadInboundMime"
    effect = "Allow"
    actions = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.inbound.arn}/incoming/*"]
  }
}

resource "aws_iam_role_policy" "application_email" {
  name   = "${var.name_prefix}-ses-runtime"
  role   = var.application_role_name
  policy = data.aws_iam_policy_document.application_email.json
}
