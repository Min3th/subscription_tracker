provider "aws" {
  region = var.aws_region

  allowed_account_ids = [
    "594559484604"
  ]

  default_tags {
    tags = local.common_tags
  }

}

provider "aws" {
  alias  = "ses"
  region = var.ses_region

  allowed_account_ids = [
    "594559484604"
  ]

  default_tags {
    tags = local.common_tags
  }
}
