locals {
  name_prefix = "${var.project_name}-${var.environment}"

  common_tags = {
    Application = "SubTrak"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

module "network" {
  source = "../../modules/network"

  name_prefix          = local.name_prefix
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
}

module "database" {
  source = "../../modules/database"

  name_prefix        = local.name_prefix
  vpc_id             = module.network.vpc_id
  private_subnet_ids = module.network.private_subnet_ids

  allowed_security_group_ids = { application = aws_security_group.application.id }

  database_name  = var.database_name
  instance_class = var.database_instance_class

  engine_version        = "18.3"
  allocated_storage     = 20
  max_allocated_storage = 50

  backup_retention_days = 1
  multi_az              = false
  deletion_protection   = false
  skip_final_snapshot   = true
  apply_immediately     = true
}

resource "aws_security_group" "application" {
  name_prefix = "${local.name_prefix}-application-"
  description = "Security group for the SubTrak dev backend"
  vpc_id      = module.network.vpc_id

  tags = {
    Name = "${local.name_prefix}-application-sg"
  }

  lifecycle {
    create_before_destroy = true
  }
}
