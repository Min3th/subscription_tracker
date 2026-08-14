terraform {
  backend "s3" {
    bucket = "BOOTSTRAP-STATE-BUCKET"
    key = "subtrak/dev/terraform.tfstate"
    region = "ap-south-1"
    encrypt = true
    use_lockfile = true
  }
}
